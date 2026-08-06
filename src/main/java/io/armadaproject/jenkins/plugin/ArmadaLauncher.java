package io.armadaproject.jenkins.plugin;

import api.EventOuterClass.JobPendingEvent;
import api.Job.JobStatusRequest;
import api.Job.JobStatusResponse;
import api.SubmitOuterClass.JobState;
import api.SubmitOuterClass.JobSubmitRequest;
import api.SubmitOuterClass.JobSubmitResponse;
import hudson.model.TaskListener;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import io.armadaproject.ArmadaClient;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

public class ArmadaLauncher extends JNLPLauncher {

  private static final Logger LOGGER = Logger.getLogger(ArmadaLauncher.class.getName());

  private final AtomicBoolean launched = new AtomicBoolean(false);

  public ArmadaLauncher() {
    super();
  }

  @Override
  public boolean isLaunchSupported() {
    return !launched.get();
  }

  @Override
  public synchronized void launch(SlaveComputer computer, TaskListener listener) {
    if (!(computer instanceof ArmadaComputer armadaComputer)) {
      throw new IllegalArgumentException("ArmadaLauncher can only launch ArmadaComputer instances");
    }

    computer.setAcceptingTasks(false);
    ArmadaSlave node = armadaComputer.getNode();

    if (node == null) {
      throw new IllegalStateException("Node has been removed, cannot launch " + computer.getName());
    }

    if (launched.get()) {
      LOGGER.log(Level.FINE, "Agent has already been launched, activating: {0}",
          node.getNodeName());
      computer.setAcceptingTasks(true);
      return;
    }

    try {
      // Use the listener from the template to ensure logs go to pipeline console
      TaskListener effectiveListener = listener;
      ArmadaJobTemplate template = node.getTemplate();
      if (template != null) {
        effectiveListener = template.getListener();
        LOGGER.fine("Using listener from ArmadaJobTemplate for pipeline console output");
      }

      submitToArmada(node, armadaComputer, effectiveListener);

      waitForAgentConnection(node, armadaComputer, effectiveListener);

      // Mark as accepting tasks and launched
      computer.setAcceptingTasks(true);
      launched.set(true);

      try {
        node.save();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Could not save() agent: " + e.getMessage(), e);
      }
    } catch (Exception e) {
      LOGGER.severe("Failed to launch Armada agent: " + e.getMessage());
      listener.error("Failed to launch Armada agent: " + e.getMessage()).close();
      e.printStackTrace(listener.getLogger());
      throw new RuntimeException(e);
    }
  }

  private void submitToArmada(ArmadaSlave node, ArmadaComputer computer, TaskListener listener)
      throws IOException {
    Pod podSpec = validateAndGetPodSpec(node);

    try {
      ArmadaCloud cloud = ArmadaCloud.resolveCloud(node.getCloudName());
      podSpec = enrichPodSpec(cloud, node, podSpec, listener);
      logArmadaConfiguration(cloud, listener);

      try (ArmadaClient armadaClient = cloud.createArmadaClient()) {
        // On recovery, the prior launch's jobSetId is persisted on the computer and is the only
        // one the surviving Armada job lives under; generating a fresh one would point the
        // pending-event subscription at an empty job set.
        boolean recovering = computer.getArmadaJobId() != null
            && !computer.getArmadaJobId().isEmpty()
            && computer.getArmadaJobSetId() != null
            && !computer.getArmadaJobSetId().isEmpty();
        String jobSetId = recovering
            ? computer.getArmadaJobSetId()
            : generateAndSetJobSetId(cloud, listener);

        // Subscribe to pending events BEFORE submitting so the event manager (which has no
        // replay) cannot drop a fast-arriving pending between submit and subscribe. The recovery
        // path runs through the same flow so a crashed prior launch (job submitted, Secret never
        // created) can still finish provisioning; createJnlpSecret tolerates an existing Secret.
        try (PendingEventSubscription pendingSub =
            PendingEventSubscription.start(cloud, jobSetId)) {
          if (!handleExistingJob(armadaClient, computer, listener)) {
            submitNewJob(armadaClient, cloud, computer, podSpec, jobSetId, listener);
          }

          // Create the per-agent JNLP Secret on the executor cluster before waiting for the
          // job to reach RUNNING. The kubelet retries unresolved secretKeyRef env on backoff,
          // so the jnlp container would otherwise block in CreateContainerConfigError forever
          // and the Armada job would never transition to RUNNING.
          provisionJnlpSecret(cloud, computer, pendingSub, listener);
        }

        waitForJobRunning(armadaClient, computer, listener);
      }
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      handleSubmissionError(e, listener);
    }
  }

  /**
   * Validates and retrieves the pod specification from the node template.
   */
  private Pod validateAndGetPodSpec(ArmadaSlave node) throws IOException {
    Pod podSpec = node.getTemplate().getPodSpec();
    if (podSpec == null) {
      throw new IOException("No pod specification available");
    }
    return podSpec;
  }

  /**
   * Enriches the pod specification with JNLP agent configuration.
   */
  private Pod enrichPodSpec(ArmadaCloud cloud, ArmadaSlave node, Pod podSpec,
      TaskListener listener) {
    listener.getLogger().println("Enriching pod with JNLP agent configuration...");
    PodEnricher enricher = new PodEnricher(cloud, node, podSpec);
    Pod enrichedPod = enricher.enrich();
    listener.getLogger().println("Pod enriched successfully");
    return enrichedPod;
  }

  /**
   * Logs the Armada cloud configuration to the listener.
   */
  private void logArmadaConfiguration(ArmadaCloud cloud, TaskListener listener) {
    listener.getLogger().println("Using Armada configuration:");
    listener.getLogger().println("  URL: " + cloud.getArmadaUrl() + ":" + cloud.getArmadaPort());
    listener.getLogger().println("  Namespace: " + cloud.getArmadaNamespace());
    listener.getLogger().println("  Queue: " + cloud.getArmadaQueue());
  }

  /**
   * Generates a new job set ID with timestamp and sets it on the cloud.
   */
  private String generateAndSetJobSetId(ArmadaCloud cloud, TaskListener listener) {
    String jobSetId = cloud.getDisplayName()
        + new SimpleDateFormat(ArmadaPluginConfig.JOB_SET_DATE_FORMAT).format(new Date());
    cloud.setArmadaJobSetId(jobSetId);
    listener.getLogger().println("  Job Set ID: " + jobSetId);
    return jobSetId;
  }

  /**
   * Handles the case where a job already exists (interrupted provisioning recovery).
   *
   * @return true if an existing job was found and handled, false otherwise
   */
  private boolean handleExistingJob(ArmadaClient armadaClient, ArmadaComputer computer,
      TaskListener listener) throws IOException {
    if (computer.getArmadaJobId() == null || computer.getArmadaJobId().isEmpty()) {
      return false;
    }

    JobStatusResponse jobStatusResponse = armadaClient.getJobStatus(
        JobStatusRequest.newBuilder()
            .addJobIds(computer.getArmadaJobId())
            .build());
    JobState existingJobState = jobStatusResponse.getJobStatesMap()
        .get(computer.getArmadaJobId());

    LOGGER.fine("Job with id: " + computer.getArmadaJobId() + " in state: " + existingJobState);

    if (existingJobState != JobState.UNKNOWN) {
      listener.getLogger().println("Job already exists: " + computer.getArmadaJobId());
      computer.setLaunching(true);
      return true;
    }

    return false;
  }

  /**
   * Submits a new job to Armada and waits for it to be running.
   */
  private void submitNewJob(ArmadaClient armadaClient, ArmadaCloud cloud, ArmadaComputer computer,
      Pod podSpec, String jobSetId, TaskListener listener) throws IOException {
    JobSubmitRequest request = createJobSubmitRequest(cloud, podSpec, jobSetId);
    logContainerInfo(podSpec, listener);

    listener.getLogger().println("Submitting job request to Armada...");
    JobSubmitResponse response = armadaClient.submitJob(request);

    String jobId = extractJobId(response);
    configureComputerWithJobInfo(computer, jobId, jobSetId);

    listener.getLogger().println("Job submitted successfully with id: " + jobId);
    logLookoutUrl(cloud, jobId, listener);
  }

  /**
   * Creates a job submit request using the ArmadaMapper.
   */
  private JobSubmitRequest createJobSubmitRequest(ArmadaCloud cloud, Pod podSpec, String jobSetId) {
    ArmadaMapper mapper = new ArmadaMapper(
        cloud.getArmadaQueue(),
        cloud.getArmadaNamespace(),
        jobSetId,
        podSpec
    );
    return mapper.createJobSubmitRequest();
  }

  /**
   * Logs container information to the listener.
   */
  private void logContainerInfo(Pod podSpec, TaskListener listener) {
    if (podSpec.getSpec() != null && podSpec.getSpec().getContainers() != null) {
      listener.getLogger().println("  Containers: " + podSpec.getSpec().getContainers().size());
      podSpec.getSpec().getContainers().forEach(container -> {
        listener.getLogger()
            .println("    - " + container.getName() + " (image: " + container.getImage() + ")");
      });
    }
  }

  /**
   * Extracts the job ID from the submit response.
   */
  private String extractJobId(JobSubmitResponse response) throws IOException {
    if (response == null || response.getJobResponseItemsList() == null ||
        response.getJobResponseItemsList().isEmpty()) {
      throw new IOException("No job ID returned from Armada");
    }
    return response.getJobResponseItems(0).getJobId();
  }

  /**
   * Configures the computer with job ID and job set ID.
   */
  private void configureComputerWithJobInfo(ArmadaComputer computer, String jobId,
      String jobSetId) {
    computer.setArmadaJobId(jobId);
    computer.setArmadaJobSetId(jobSetId);
    computer.setLaunching(true);
  }

  /**
   * Logs the Lookout URL if configured.
   */
  private void logLookoutUrl(ArmadaCloud cloud, String jobId, TaskListener listener) {
    if (cloud.getArmadaLookoutUrl() != null && !cloud.getArmadaLookoutUrl().isEmpty()) {
      String armadaLookoutJobUrl = cloud.getArmadaLookoutUrl() + ":"
          + cloud.getArmadaLookoutPort() + "/?sb=" + jobId;
      listener.getLogger().println("Lookout URL: " + armadaLookoutJobUrl);
    }
  }

  /**
   * Handles job submission errors by logging and wrapping them.
   */
  private void handleSubmissionError(Exception e, TaskListener listener) throws IOException {
    LOGGER.severe("Failed to submit job to Armada: " + e.getMessage());
    listener.error("Failed to submit job to Armada: " + e.getMessage()).close();
    throw new IOException("Failed to submit job to Armada", e);
  }

  /**
   * Gets the current state of a job from Armada.
   */
  private JobState getJobState(ArmadaClient client, String jobId) throws IOException {
    try {
      JobStatusResponse status = client.getJobStatus(
          JobStatusRequest.newBuilder().addJobIds(jobId).build());
      return status.getJobStatesMap().get(jobId);
    } catch (Exception e) {
      throw new IOException("Failed to get job status for: " + jobId, e);
    }
  }

  /**
   * Waits for the Armada job to reach RUNNING state using Armada's status API.
   */
  private void waitForJobRunning(ArmadaClient armadaClient, ArmadaComputer computer,
      TaskListener listener) throws IOException {
    String jobId = computer.getArmadaJobId();
    listener.getLogger().println("Waiting for job to be running...");

    try {
      Awaitility.await()
          .atMost(ArmadaPluginConfig.DEFAULT_SLAVE_CONNECT_TIMEOUT, TimeUnit.SECONDS)
          .pollInterval(ArmadaPluginConfig.POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
          .until(() -> {
            JobState currentState = getJobState(armadaClient, jobId);
            LOGGER.fine("Job " + jobId + " state: " + currentState);

            // Check for terminal failure states using validator
            JobStateValidator.validate(currentState, jobId);

            return JobStateValidator.isRunning(currentState);
          });

      listener.getLogger().println("Job is running: " + jobId);

    } catch (Exception e) {
      LOGGER.severe("Job failed to reach RUNNING state: " + e.getMessage());
      listener.error("Job failed to reach RUNNING state: " + e.getMessage()).close();
      throw new IOException("Job did not start successfully: " + jobId, e);
    }
  }

  /**
   * Awaits the JobPendingEvent (the earliest event carrying clusterId/podName/podNamespace)
   * via the pre-attached subscription, then creates the per-agent JNLP Secret on the executor
   * cluster, owned by the pod so it is garbage-collected with it. Must run before
   * waitForJobRunning because the jnlp container cannot start (and the job cannot reach RUNNING)
   * until this Secret exists.
   */
  private void provisionJnlpSecret(ArmadaCloud cloud, ArmadaComputer computer,
      PendingEventSubscription pendingSub, TaskListener listener) throws IOException {
    String jobId = computer.getArmadaJobId();

    listener.getLogger().println("Waiting for pending event to provision JNLP Secret...");
    JobPendingEvent event = pendingSub.awaitFor(jobId);
    String serverUrl = ArmadaNodeContext.resolveServerUrl(cloud, event.getClusterId());
    String namespace = event.getPodNamespace();
    String podName = event.getPodName();
    String secretName = computer.getName() + ArmadaPluginConfig.JNLP_SECRET_NAME_SUFFIX;

    try (KubernetesClient client = cloud.connect(serverUrl, namespace)) {
      Pod pod = awaitPodWithUid(client, namespace, podName);
      createJnlpSecret(client, pod, computer.getJnlpMac(), secretName);
      listener.getLogger().println("JNLP Secret created: " + namespace + "/" + secretName);
    }
  }

  /**
   * Polls until the Pod object exists on the executor cluster and has a non-null UID. The Pod's
   * UID is required to set a controller ownerReference on the Secret (so it is garbage-collected
   * with the Pod). JobPending in Armada means the job has been assigned to a cluster, but the
   * Pod object on that cluster may not be visible to the API server yet.
   */
  private static Pod awaitPodWithUid(KubernetesClient client, String namespace, String podName)
      throws IOException {
    try {
      return Awaitility.await()
          .atMost(ArmadaPluginConfig.POD_UID_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .pollInterval(ArmadaPluginConfig.POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
          .until(() -> client.pods().inNamespace(namespace).withName(podName).get(),
              pod -> pod != null && pod.getMetadata() != null
                  && pod.getMetadata().getUid() != null);
    } catch (ConditionTimeoutException e) {
      throw new IOException("Timed out waiting for pod " + namespace + "/" + podName
          + " to appear on the executor cluster", e);
    }
  }

  /**
   * Creates a per-agent Kubernetes Secret holding the JNLP HMAC, owned by the Pod so it is
   * garbage-collected with it. The Secret is referenced by the jnlp container via secretKeyRef
   * (see PodEnricher), so the value never appears in the PodSpec.
   */
  private static void createJnlpSecret(KubernetesClient client, Pod pod, String jnlpMac,
      String secretName) {
    Secret secret = buildJnlpSecret(pod, jnlpMac, secretName);
    String namespace = secret.getMetadata().getNamespace();
    try {
      client.secrets().inNamespace(namespace).create(secret);
      LOGGER.fine(() -> "Created JNLP Secret: " + namespace + "/" + secretName);
    } catch (KubernetesClientException e) {
      if (e.getCode() == 409) {
        // Defensive: an orphaned Secret can exist if a prior launch attempt crashed between
        // create-Secret and create-Pod. The JNLP mac is deterministic per agent identity, so
        // reusing the existing Secret is safe.
        LOGGER.fine(() -> "JNLP Secret already exists, reusing: " + namespace + "/" + secretName);
      } else {
        throw e;
      }
    }
  }

  /**
   * Builds the Secret object referenced by the jnlp container's secretKeyRef. Extracted as a
   * pure function for testability — the result is what actually carries the JNLP HMAC.
   */
  static Secret buildJnlpSecret(Pod pod, String jnlpMac, String secretName) {
    return new SecretBuilder()
        .withNewMetadata()
        .withName(secretName)
        .withNamespace(pod.getMetadata().getNamespace())
        .addToOwnerReferences(new OwnerReferenceBuilder()
            .withApiVersion("v1")
            .withKind("Pod")
            .withName(pod.getMetadata().getName())
            .withUid(pod.getMetadata().getUid())
            .withController(true)
            .withBlockOwnerDeletion(true)
            .build())
        .endMetadata()
        .withType("Opaque")
        .addToStringData(ArmadaPluginConfig.JNLP_SECRET_KEY, jnlpMac)
        .build();
  }

  /**
   * Waits for the JNLP agent to connect back to Jenkins.
   */
  private void waitForAgentConnection(ArmadaSlave node, ArmadaComputer computer,
      TaskListener listener) throws IOException {
    listener.getLogger().println("Waiting for agent to connect...");

    try {
      ArmadaCloud cloud = ArmadaCloud.resolveCloud(node.getCloudName());
      try (ArmadaClient armadaClient = cloud.createArmadaClient()) {
        pollForAgentConnection(node, computer, armadaClient, listener);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting for agent connection", e);
    }
  }

  /**
   * Polls for agent connection with periodic status checks and reporting.
   */
  private void pollForAgentConnection(ArmadaSlave node, ArmadaComputer computer,
      ArmadaClient armadaClient, TaskListener listener)
      throws InterruptedException, IOException {
    long lastReportTimestamp = System.currentTimeMillis();

    for (int waitedSeconds = 0; waitedSeconds < ArmadaPluginConfig.DEFAULT_SLAVE_CONNECT_TIMEOUT;
        waitedSeconds++) {
      if (checkAgentConnected(node, waitedSeconds, listener)) {
        return;
      }

      validateJobStillRunning(computer, armadaClient);

      lastReportTimestamp = reportProgressIfNeeded(node, waitedSeconds, lastReportTimestamp,
          listener);

      Thread.sleep(ArmadaPluginConfig.AGENT_CONNECTION_POLL_INTERVAL_MS);
    }

    throw new IllegalStateException(
        "Agent did not connect after " + ArmadaPluginConfig.DEFAULT_SLAVE_CONNECT_TIMEOUT
            + " seconds");
  }

  /**
   * Checks if the agent has connected successfully.
   *
   * @return true if agent is online, false otherwise
   */
  private boolean checkAgentConnected(ArmadaSlave node, int waitedSeconds, TaskListener listener) {
    SlaveComputer slaveComputer = node.getComputer();

    if (slaveComputer == null) {
      throw new IllegalStateException("Node was deleted, computer is null");
    }

    if (slaveComputer.isOnline()) {
      listener.getLogger()
          .println("Agent connected successfully after " + waitedSeconds + " seconds");
      return true;
    }

    return false;
  }

  /**
   * Validates that the job hasn't failed during the wait period.
   */
  private void validateJobStillRunning(ArmadaComputer computer, ArmadaClient armadaClient)
      throws IOException {
    String jobId = computer.getArmadaJobId();
    if (jobId != null && !jobId.isEmpty()) {
      JobState jobState = getJobState(armadaClient, jobId);
      JobStateValidator.validate(jobState, jobId);
    }
  }

  /**
   * Reports connection progress periodically.
   *
   * @return the updated timestamp if report was made, original timestamp otherwise
   */
  private long reportProgressIfNeeded(ArmadaSlave node, int waitedSeconds,
      long lastReportTimestamp, TaskListener listener) {
    if (lastReportTimestamp + ArmadaPluginConfig.REPORT_INTERVAL_MS < System.currentTimeMillis()) {
      LOGGER.log(Level.INFO, "Waiting for agent to connect ({1}/{2}): {0}",
          new Object[]{node.getNodeName(), waitedSeconds,
              ArmadaPluginConfig.DEFAULT_SLAVE_CONNECT_TIMEOUT});
      listener.getLogger().printf(
          "Waiting for agent to connect (%2$s/%3$s): %1$s%n",
          node.getNodeName(), waitedSeconds, ArmadaPluginConfig.DEFAULT_SLAVE_CONNECT_TIMEOUT);
      return System.currentTimeMillis();
    }
    return lastReportTimestamp;
  }
}
