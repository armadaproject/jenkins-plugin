package io.armadaproject.jenkins.plugin;

import api.EventOuterClass.EventMessage;
import api.EventOuterClass.EventStreamMessage;
import api.EventOuterClass.JobPendingEvent;
import api.EventOuterClass.JobRunningEvent;
import api.EventOuterClass.JobSetRequest;
import api.Health.HealthCheckResponse.ServingStatus;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import io.armadaproject.ArmadaClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class ArmadaCloud extends Cloud {

  private static final Logger LOGGER = Logger.getLogger(ArmadaCloud.class.getName());

  private transient Map<String, ArmadaJobTemplate> dynamicTemplates = new ConcurrentHashMap<>();
  private transient ArmadaEventManager<JobRunningEvent> armadaEventManager;
  private transient ArmadaEventManager<JobPendingEvent> armadaPendingEventManager;
  private transient ConcurrentHashMap<String, Thread> jobSetIdThreads;

  private String armadaUrl;
  private String armadaPort;
  private String armadaQueue;
  private String armadaNamespace;
  private String armadaCredentialsId;
  private String armadaLookoutUrl;
  private String armadaLookoutPort;
  private String armadaJobSetPrefix;
  private String armadaJobSetId;
  private String armadaClusterConfigPath;
  private String jenkinsUrl;
  private boolean trustCerts = false;

  @DataBoundConstructor
  public ArmadaCloud(String name) {
    super(name);
  }

  public static ArmadaCloud resolveCloud(String cloudName) throws IOException {
    if (cloudName == null) {
      ArmadaCloud cloud = Jenkins.get().clouds.get(ArmadaCloud.class);
      if (cloud == null) {
        throw new IOException("No Armada cloud was found.");
      }
      return cloud;
    }

    Cloud cloud = Jenkins.get().getCloud(cloudName);
    if (cloud == null) {
      throw new IOException(String.format("Cloud does not exist: %s", cloudName));
    }

    if (!(cloud instanceof ArmadaCloud)) {
      throw new IOException(String.format(
          "Cloud is not an Armada cloud: %s (%s)",
          cloudName, cloud.getClass().getName()));
    }

    return (ArmadaCloud) cloud;
  }

  /**
   * Resolves credentials by ID from Jenkins credentials store.
   *
   * @param credentialsId the ID of the credentials to resolve
   * @return the StandardCredentials object, or null if not found
   */
  @CheckForNull
  private static StandardCredentials resolveCredentials(@CheckForNull String credentialsId) {
    if (credentialsId == null || credentialsId.trim().isEmpty()) {
      return null;
    }
    return CredentialsMatchers.firstOrNull(
        CredentialsProvider.lookupCredentialsInItemGroup(
            StandardCredentials.class, Jenkins.get(), ACL.SYSTEM2, Collections.emptyList()),
        CredentialsMatchers.withId(credentialsId));
  }

  /**
   * Called after deserialization to initialize transient fields.
   */
  protected Object readResolve() {
    if (dynamicTemplates == null) {
      dynamicTemplates = new ConcurrentHashMap<>();
    }
    if (armadaEventManager == null) {
      armadaEventManager = new ArmadaEventManager<>();
    }
    if (armadaPendingEventManager == null) {
      armadaPendingEventManager = new ArmadaEventManager<>();
    }
    if (jobSetIdThreads == null) {
      jobSetIdThreads = new ConcurrentHashMap<>();
    }
    return this;
  }

  public String getArmadaUrl() {
    return armadaUrl;
  }

  @DataBoundSetter
  public void setArmadaUrl(String armadaUrl) {
    this.armadaUrl = armadaUrl;
  }

  public String getArmadaPort() {
    return armadaPort;
  }

  @DataBoundSetter
  public void setArmadaPort(String armadaPort) {
    this.armadaPort = armadaPort;
  }

  public String getArmadaQueue() {
    return armadaQueue;
  }

  @DataBoundSetter
  public void setArmadaQueue(String armadaQueue) {
    this.armadaQueue = armadaQueue;
  }

  public String getArmadaNamespace() {
    return armadaNamespace;
  }

  @DataBoundSetter
  public void setArmadaNamespace(String armadaNamespace) {
    this.armadaNamespace = armadaNamespace;
  }

  public String getArmadaCredentialsId() {
    return armadaCredentialsId;
  }

  @DataBoundSetter
  public void setArmadaCredentialsId(String armadaCredentialsId) {
    this.armadaCredentialsId = armadaCredentialsId;
  }

  public String getArmadaLookoutUrl() {
    return armadaLookoutUrl;
  }

  @DataBoundSetter
  public void setArmadaLookoutUrl(String armadaLookoutUrl) {
    this.armadaLookoutUrl = armadaLookoutUrl;
  }

  public String getArmadaLookoutPort() {
    return armadaLookoutPort;
  }

  @DataBoundSetter
  public void setArmadaLookoutPort(String armadaLookoutPort) {
    this.armadaLookoutPort = armadaLookoutPort;
  }

  public String getArmadaJobSetPrefix() {
    return armadaJobSetPrefix;
  }

  @DataBoundSetter
  public void setArmadaJobSetPrefix(String armadaJobSetPrefix) {
    this.armadaJobSetPrefix = armadaJobSetPrefix;
  }

  public String getArmadaJobSetId() {
    return armadaJobSetId;
  }

  @DataBoundSetter
  public void setArmadaJobSetId(String armadaJobSetId) {
    this.armadaJobSetId = armadaJobSetId;
  }

  public String getArmadaClusterConfigPath() {
    return armadaClusterConfigPath;
  }

  @DataBoundSetter
  public void setArmadaClusterConfigPath(String armadaClusterConfigPath) {
    this.armadaClusterConfigPath = armadaClusterConfigPath;
  }

  public String getJenkinsUrl() {
    return jenkinsUrl;
  }

  @DataBoundSetter
  public void setJenkinsUrl(String jenkinsUrl) {
    this.jenkinsUrl = jenkinsUrl;
  }

  public boolean getTrustCerts() {
    return trustCerts;
  }

  @DataBoundSetter
  public void setTrustCerts(boolean trustCerts) {
    this.trustCerts = trustCerts;
  }

  public int getRetentionTimeout() {
    return ArmadaPluginConfig.DEFAULT_RETENTION_TIMEOUT_MINUTES;
  }

  /**
   * Validates that all required configuration is present and valid. This is called before the cloud
   * is used for provisioning.
   *
   * @throws IllegalStateException if configuration is invalid
   */
  private void validateConfiguration() {
    String cloudContext = " for cloud: " + name;

    try {
      ValidationHelper.validateRequired(armadaUrl, "Armada Server URL");
    } catch (IllegalStateException e) {
      throw new IllegalStateException(e.getMessage() + cloudContext, e);
    }

    try {
      ValidationHelper.validatePort(armadaPort, true, "Armada Server Port");
    } catch (IllegalStateException e) {
      throw new IllegalStateException(e.getMessage() + cloudContext, e);
    }

    try {
      ValidationHelper.validateRequired(armadaQueue, "Armada Queue");
    } catch (IllegalStateException e) {
      throw new IllegalStateException(e.getMessage() + cloudContext, e);
    }

    try {
      ValidationHelper.validateRequired(armadaNamespace, "Armada Namespace");
    } catch (IllegalStateException e) {
      throw new IllegalStateException(e.getMessage() + cloudContext, e);
    }

    try {
      ValidationHelper.validateRequired(armadaClusterConfigPath, "Cluster Config Path");
    } catch (IllegalStateException e) {
      throw new IllegalStateException(e.getMessage() + cloudContext, e);
    }

    try {
      ValidationHelper.validateRequired(jenkinsUrl, "Jenkins URL");
    } catch (IllegalStateException e) {
      throw new IllegalStateException(e.getMessage() + cloudContext, e);
    }

    // Validate optional lookout port if provided
    try {
      ValidationHelper.validatePort(armadaLookoutPort, false, "Armada Lookout Port");
    } catch (IllegalStateException e) {
      throw new IllegalStateException(e.getMessage() + cloudContext, e);
    }
  }

  /**
   * Checks if this cloud can provision nodes for the given state.
   */
  @Override
  public boolean canProvision(CloudState state) {
    String label = state.getLabel() != null ? state.getLabel().toString() : null;
    if (label == null) {
      return false;
    }
    // Check if we have a dynamic template for this label
    return dynamicTemplates.containsKey(label);
  }

  @Override
  public Collection<PlannedNode> provision(CloudState state, int excessWorkload) {
    // Validate configuration before attempting to provision
    validateConfiguration();

    String label = state.getLabel() != null ? state.getLabel().toString() : null;
    if (label == null) {
      LOGGER.log(Level.WARNING, "No label provided for provisioning");
      return new ArrayList<>();
    }

    // Look up the dynamic template for this label
    ArmadaJobTemplate template = dynamicTemplates.get(label);
    if (template == null) {
      LOGGER.log(Level.WARNING, "No pod template found for label: " + label);
      return new ArrayList<>();
    }

    Collection<PlannedNode> result = new ArrayList<>();

    // Create a planned node for each requested workload
    // Continue creating nodes even if some fail
    for (int i = 0; i < excessWorkload; i++) {
      try {
        PlannedNode node = createPlannedNode(template, i + 1, excessWorkload);
        result.add(node);
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE,
            "Failed to create planned node " + (i + 1) + "/" + excessWorkload +
                " for label: " + label, e);
        // Continue creating other nodes even if one fails
      }
    }

    if (result.isEmpty() && excessWorkload > 0) {
      LOGGER.log(Level.SEVERE, "Failed to create any planned nodes for label: " + label);
    } else if (result.size() < excessWorkload) {
      LOGGER.log(Level.WARNING, "Created " + result.size() + "/" + excessWorkload +
          " planned nodes for label: " + label);
    } else {
      LOGGER.log(Level.FINE, "Created " + result.size() + " planned nodes for label: " + label);
    }

    return result;
  }

  /**
   * Creates a single planned node for the given template.
   *
   * @param template   the template to use
   * @param nodeIndex  the index of this node (for logging)
   * @param totalNodes the total number of nodes being created (for logging)
   * @return the planned node
   */
  private PlannedNode createPlannedNode(ArmadaJobTemplate template, int nodeIndex, int totalNodes) {
    Callable<hudson.model.Node> callable = createNodeCallable(template, nodeIndex, totalNodes);
    FutureTask<Node> future = new FutureTask<>(callable);

    PlannedNode node = new PlannedNode(template.getLabel(), future, 1);

    // Execute the task immediately
    future.run();

    return node;
  }

  /**
   * Creates a callable that provisions the actual node.
   *
   * @param template   the template to use
   * @param nodeIndex  the index of this node (for logging)
   * @param totalNodes the total number of nodes being created (for logging)
   * @return the callable that creates the node
   */
  private Callable<hudson.model.Node> createNodeCallable(ArmadaJobTemplate template,
      int nodeIndex, int totalNodes) {
    return () -> {
      try {
        LOGGER.log(Level.FINE, "Creating Armada slave " + nodeIndex + "/" + totalNodes +
            " for label: " + template.getLabel());

        ArmadaSlave slave = new ArmadaSlave(this, template);

        // Add the slave to Jenkins
        Jenkins.get().addNode(slave);

        LOGGER.log(Level.FINE, "Successfully created Armada slave " + nodeIndex + "/" + totalNodes +
            " for label: " + template.getLabel());

        return slave;
      } catch (IOException e) {
        throw e;
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE,
            "Failed to create Armada slave " + nodeIndex + "/" + totalNodes, e);
        throw new IOException("Failed to provision Armada agent", e);
      }
    };
  }

  public void addDynamicTemplate(ArmadaJobTemplate template) {
    LOGGER.log(Level.FINE, "Adding dynamic template for label: " + template.getLabel());
    dynamicTemplates.put(template.getLabel(), template);
  }

  public void removeDynamicTemplate(String label) {
    LOGGER.log(Level.FINE, "Removing dynamic template for label: " + label);
    dynamicTemplates.remove(label);
  }

  /**
   * Gets the event manager, creating it lazily if needed.
   */
  public ArmadaEventManager<JobRunningEvent> getArmadaEventManager() {
    if (armadaEventManager == null) {
      armadaEventManager = new ArmadaEventManager<>();
    }
    return armadaEventManager;
  }

  /**
   * Gets the JobPendingEvent manager, creating it lazily if needed. The pending event is the
   * earliest event that carries clusterId/podName/podNamespace, used to create per-agent JNLP
   * Secrets before the kubelet needs them.
   */
  public ArmadaEventManager<JobPendingEvent> getArmadaPendingEventManager() {
    if (armadaPendingEventManager == null) {
      armadaPendingEventManager = new ArmadaEventManager<>();
    }
    return armadaPendingEventManager;
  }

  /**
   * Gets the job set threads map, creating it lazily if needed.
   */
  public ConcurrentHashMap<String, Thread> getJobSetIdThreads() {
    if (jobSetIdThreads == null) {
      jobSetIdThreads = new ConcurrentHashMap<>();
    }
    return jobSetIdThreads;
  }

  /**
   * Starts watching Armada events for the specified job set ID. Creates a background thread that
   * streams events from Armada and publishes them to subscribers.
   *
   * @param jobSetId the job set ID to watch
   * @return the thread watching the events
   */
  public Thread startWatchingArmadaEvents(String jobSetId) {
    Runnable job = () -> {
      try (ArmadaClient armadaClient = createArmadaClient()) {
        JobSetRequest jobSetRequest = JobSetRequest.newBuilder()
            .setId(jobSetId)
            .setQueue(armadaQueue)
            .setWatch(true)
            .build();

        StreamObserver<EventStreamMessage> streamObserver = new StreamObserver<>() {
          @Override
          public void onNext(EventStreamMessage value) {
            LOGGER.log(Level.FINE,
                "Event received for jobSetId: " + jobSetId + " message: " + value);
            if (!value.hasMessage()) {
              return;
            }

            EventMessage message = value.getMessage();
            if (message.hasPending()) {
              getArmadaPendingEventManager().publish(jobSetId, message.getPending());
              return;
            }
            if (!message.hasRunning()) {
              return;
            }

            JobRunningEvent jobRunningEvent = message.getRunning();
            getArmadaEventManager().publish(jobSetId, jobRunningEvent);
          }

          @Override
          public void onError(Throwable t) {
            LOGGER.log(Level.SEVERE, "Error received for jobSetId: " + jobSetId + " error: " + t);
          }

          @Override
          public void onCompleted() {
            LOGGER.log(Level.FINE, "Streaming completed for jobSetId: " + jobSetId);
          }
        };

        armadaClient.streamEvents(jobSetRequest, streamObserver);
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE,
            "Failed to connect to Armada. Could not start watching events.", e);
      }
    };
    Thread watcher = new Thread(job);
    watcher.setName("armada-event-watcher-" + jobSetId);
    watcher.setDaemon(true);
    getJobSetIdThreads().put(jobSetId, watcher);
    watcher.start();
    return watcher;
  }

  /**
   * Creates a new Armada client configured with this cloud's connection settings. If credentials
   * are configured, creates a secure connection. Otherwise, creates an insecure connection.
   *
   * @return a new ArmadaClient instance (caller must close it)
   * @throws IllegalStateException if armadaUrl or armadaPort are not properly configured
   */
  public ArmadaClient createArmadaClient() {
    if (armadaUrl == null || armadaUrl.trim().isEmpty()) {
      throw new IllegalStateException("Armada URL is not configured for cloud: " + name);
    }
    if (armadaPort == null || armadaPort.trim().isEmpty()) {
      throw new IllegalStateException("Armada port is not configured for cloud: " + name);
    }

    try {
      int port = Integer.parseInt(armadaPort);
      if (port < 1 || port > 65535) {
        throw new IllegalStateException(
            "Armada port must be between 1 and 65535, got: " + port + " for cloud: " + name);
      }

      // Check if credentials are configured
      if (armadaCredentialsId != null && !armadaCredentialsId.trim().isEmpty()) {
        StandardCredentials standardCredentials = resolveCredentials(armadaCredentialsId);
        if (standardCredentials == null) {
          throw new IllegalStateException(
              "Credentials with ID '" + armadaCredentialsId + "' not found for cloud: " + name);
        }
        if (!(standardCredentials instanceof StringCredentials)) {
          throw new IllegalStateException(
              "Credentials with ID '" + armadaCredentialsId
                  + "' are not String credentials for cloud: " + name);
        }

        String secret = ((StringCredentials) standardCredentials).getSecret().getPlainText();
        LOGGER.log(Level.FINE, "Creating secure Armada client with credentials for cloud: " + name);
        return new ArmadaClient(armadaUrl, port, secret);
      } else {
        LOGGER.log(Level.FINE,
            "Creating insecure Armada client (no credentials) for cloud: " + name);
        return new ArmadaClient(armadaUrl, port);
      }
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Armada port is not a valid number: " + armadaPort + " for cloud: " + name, e);
    }
  }

  /**
   * Connects to a Kubernetes cluster using the provided server URL.
   * <p>
   * WARNING: By default, TLS certificate validation is enabled for security. Only disable
   * certificate validation (trustCerts=true) in development/test environments with self-signed
   * certificates. Production environments should use properly signed certificates.
   *
   * @param serverUrl the Kubernetes API server URL
   * @param namespace the namespace to use
   * @return a Kubernetes client connected to the cluster
   */
  public KubernetesClient connect(String serverUrl, String namespace) {
    Config config = new ConfigBuilder()
        .withMasterUrl(serverUrl)
        .withNamespace(namespace)
        .withTrustCerts(trustCerts)
        .build();

    if (trustCerts) {
      LOGGER.log(Level.WARNING,
          "TLS certificate validation is disabled for cluster: " + serverUrl +
              ". This should only be used in development/test environments.");
    }

    return new KubernetesClientBuilder()
        .withConfig(config)
        .build();
  }


  /**
   * Validation helper methods - shared between UI validation and runtime validation.
   */
  private static class ValidationHelper {

    /**
     * Validates that a field is not empty.
     *
     * @param value     the value to check
     * @param fieldName the name of the field for error messages
     * @throws IllegalStateException if the field is empty
     */
    static void validateRequired(String value, String fieldName) {
      if (value == null || value.trim().isEmpty()) {
        throw new IllegalStateException(fieldName + " is not configured");
      }
    }

    /**
     * Validates a port number value.
     *
     * @param value     the port number as a string
     * @param required  whether the port is required or optional
     * @param fieldName the name of the field for error messages
     * @throws IllegalStateException if the port is invalid
     */
    static void validatePort(String value, boolean required, String fieldName) {
      if (value == null || value.trim().isEmpty()) {
        if (required) {
          throw new IllegalStateException(fieldName + " is not configured");
        }
        return;
      }
      try {
        int port = Integer.parseInt(value);
        if (port < 1 || port > 65535) {
          throw new IllegalStateException(
              fieldName + " must be between 1 and 65535, got: " + port);
        }
      } catch (NumberFormatException e) {
        throw new IllegalStateException(
            fieldName + " is not a valid number: " + value, e);
      }
    }
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Cloud> {

    @Override
    @Nonnull
    public String getDisplayName() {
      return "Armada";
    }

    /**
     * Fills the dropdown for Armada credentials selection in the UI.
     */
    @POST
    public hudson.util.ListBoxModel doFillArmadaCredentialsIdItems() {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      com.cloudbees.plugins.credentials.common.StandardListBoxModel result =
          new com.cloudbees.plugins.credentials.common.StandardListBoxModel();
      result.includeEmptyValue();
      result.includeAs(
          ACL.SYSTEM2,
          Jenkins.get(),
          StandardCredentials.class,
          Collections.emptyList());
      return result;
    }

    /**
     * Validates a field using the validation helper and converts exceptions to FormValidation.
     */
    private FormValidation validateField(Runnable validator) {
      try {
        validator.run();
        return FormValidation.ok();
      } catch (IllegalStateException e) {
        return FormValidation.error(e.getMessage());
      }
    }

    public FormValidation doCheckName(@QueryParameter String value) {
      return validateField(() -> ValidationHelper.validateRequired(value, "Cloud name"));
    }

    public FormValidation doCheckArmadaUrl(@QueryParameter String value) {
      return validateField(() -> ValidationHelper.validateRequired(value, "Armada Server URL"));
    }

    public FormValidation doCheckArmadaPort(@QueryParameter String value) {
      return validateField(() -> ValidationHelper.validatePort(value, true, "Armada Server Port"));
    }

    public FormValidation doCheckArmadaQueue(@QueryParameter String value) {
      return validateField(() -> ValidationHelper.validateRequired(value, "Armada Queue"));
    }

    public FormValidation doCheckArmadaNamespace(@QueryParameter String value) {
      return validateField(() -> ValidationHelper.validateRequired(value, "Armada Namespace"));
    }

    public FormValidation doCheckArmadaLookoutPort(@QueryParameter String value) {
      return validateField(
          () -> ValidationHelper.validatePort(value, false, "Armada Lookout Port"));
    }

    public FormValidation doCheckArmadaClusterConfigPath(@QueryParameter String value) {
      return validateField(() -> ValidationHelper.validateRequired(value, "Cluster Config Path"));
    }

    public FormValidation doCheckJenkinsUrl(@QueryParameter String value) {
      return validateField(() -> ValidationHelper.validateRequired(value, "Jenkins URL"));
    }

    @POST
    public FormValidation doTestArmadaConnection(@QueryParameter String armadaUrl,
        @QueryParameter String armadaPort,
        @QueryParameter String armadaCredentialsId) {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);

      // Validate inputs using shared validation logic
      try {
        ValidationHelper.validateRequired(armadaUrl, "Armada Server URL");
        ValidationHelper.validatePort(armadaPort, true, "Armada Server Port");
      } catch (IllegalStateException e) {
        return FormValidation.error(e.getMessage());
      }

      ArmadaClient armadaClient = null;
      try {
        // Check if credentials are provided
        if (armadaCredentialsId != null && !armadaCredentialsId.trim().isEmpty()) {
          StandardCredentials standardCredentials = resolveCredentials(
              Util.fixEmpty(armadaCredentialsId));
          if (standardCredentials == null) {
            return FormValidation.error(
                "Credentials with ID '%s' not found", armadaCredentialsId);
          }
          if (!(standardCredentials instanceof StringCredentials)) {
            return FormValidation.error(
                "Credentials with ID '%s' are not String credentials", armadaCredentialsId);
          }

          String secret = ((StringCredentials) standardCredentials).getSecret().getPlainText();
          armadaClient = new ArmadaClient(armadaUrl, Integer.parseInt(armadaPort), secret);
        } else {
          armadaClient = new ArmadaClient(armadaUrl, Integer.parseInt(armadaPort));
        }

        if (ServingStatus.SERVING == armadaClient.checkHealth()) {
          return FormValidation.ok("Connected to Armada");
        }

        return FormValidation.error("Connection to Armada failed %s:%s", armadaUrl, armadaPort);
      } catch (Exception e) {
        LOGGER.log(Level.FINE,
            String.format("Error testing Armada connection %s:%s", armadaUrl, armadaPort), e);
        return FormValidation.error(
            "Error testing Armada connection url:%s, port:%s, cause:%s", armadaUrl, armadaPort,
            e.getCause() != null ? e.getCause().toString() : e.getMessage());
      } finally {
        if (armadaClient != null) {
          try {
            armadaClient.close();
          } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing Armada client", e);
          }
        }
      }
    }
  }
}
