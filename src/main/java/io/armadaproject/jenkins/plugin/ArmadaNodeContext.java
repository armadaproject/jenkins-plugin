package io.armadaproject.jenkins.plugin;

import api.EventOuterClass.JobRunningEvent;
import hudson.AbortException;
import hudson.model.Node;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.steps.StepContext;

/**
 * Helper class for steps running in an Armada node context. This class subscribes to Armada events
 * and waits for the job to reach running state, then extracts pod information from the
 * JobRunningEvent to connect to the Kubernetes cluster. Implements AutoCloseable to properly manage
 * KubernetesClient resources.
 */
public class ArmadaNodeContext implements Serializable, AutoCloseable {

  @Serial
  private static final long serialVersionUID = 2L;
  private static final Logger LOGGER = Logger.getLogger(ArmadaNodeContext.class.getName());
  private final String cloudName;
  private final String nodeName;
  private transient StepContext context;
  private transient KubernetesClient client;
  private String podName;
  private String namespace;

  public ArmadaNodeContext(StepContext context) throws IOException, InterruptedException {
    this.context = context;
    ArmadaSlave agent = getArmadaSlave();
    // Pod name and namespace will be set from Armada event when connectToCloud() is called
    this.podName = null;
    this.namespace = null;
    this.cloudName = agent.getCloudName();
    this.nodeName = agent.getNodeName();
  }

  public String getPodName() {
    return podName;
  }

  public String getNamespace() {
    return namespace;
  }

  public KubernetesClient connectToCloud() throws IOException {
    if (client != null) {
      return client;
    }

    try {
      ArmadaSlave armadaSlave = resolveArmadaSlave();
      ArmadaCloud armadaCloud = ArmadaCloud.resolveCloud(cloudName);
      ArmadaComputer computer = getArmadaComputer(armadaSlave);

      String jobId = validateAndGetJobId(computer);
      String jobSetId = validateAndGetJobSetId(computer);

      ArmadaEventWaiter eventWaiter = new ArmadaEventWaiter(armadaCloud);
      JobRunningEvent event = eventWaiter.waitForJobRunning(jobId, jobSetId);

      String serverUrl = resolveServerUrl(armadaCloud, event.getClusterId());

      podName = event.getPodName();
      namespace = event.getPodNamespace();

      client = armadaCloud.connect(serverUrl, namespace);
      return client;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while connecting to cloud", e);
    }
  }

  /**
   * Resolves the Kubernetes server URL for the given cluster ID. Package-private static so
   * {@link ArmadaLauncher} can resolve the URL from a {@link JobRunningEvent}/{@code
   * JobPendingEvent} without holding a {@link StepContext}.
   */
  static String resolveServerUrl(ArmadaCloud armadaCloud, String clusterId) throws IOException {
    String serverUrl;
    try {
      serverUrl = ClusterConfigParser.parse(armadaCloud.getArmadaClusterConfigPath())
          .get(clusterId);
    } catch (IOException e) {
      throw e;
    } catch (Exception ex) {
      throw new IOException(String.format("Cluster configuration error at '%s': Failed to parse cluster config file",
          armadaCloud.getArmadaClusterConfigPath()), ex);
    }

    if (serverUrl == null || serverUrl.isEmpty()) {
      throw new IOException(String.format("Cluster configuration error at '%s': No server URL found for cluster: %s",
          armadaCloud.getArmadaClusterConfigPath(), clusterId));
    }
    return serverUrl;
  }

  /**
   * Resolves the ArmadaSlave from either the step context or Jenkins node registry.
   */
  private ArmadaSlave resolveArmadaSlave() throws IOException, InterruptedException {
    if (context != null) {
      return getArmadaSlave();
    }

    // After deserialization, context is null, so get the slave from Jenkins
    hudson.model.Node node = jenkins.model.Jenkins.get().getNode(nodeName);
    if (!(node instanceof ArmadaSlave)) {
      throw new IOException("Node is not an Armada slave: " + nodeName);
    }
    return (ArmadaSlave) node;
  }

  /**
   * Gets the ArmadaComputer for the slave, with validation.
   */
  private ArmadaComputer getArmadaComputer(ArmadaSlave armadaSlave) throws IOException {
    ArmadaComputer computer = (ArmadaComputer) armadaSlave.getComputer();
    if (computer == null) {
      throw new IOException("Computer is null for slave: " + nodeName);
    }
    return computer;
  }

  /**
   * Validates and retrieves the Armada job ID from the computer.
   */
  private String validateAndGetJobId(ArmadaComputer computer) throws IOException {
    String jobId = computer.getArmadaJobId();
    if (jobId == null || jobId.isEmpty()) {
      throw new IOException(String.format("Invalid configuration for 'jobId' with value '%s': No job ID available for slave: %s",
          jobId, nodeName));
    }
    return jobId;
  }

  /**
   * Validates and retrieves the Armada job set ID from the computer.
   */
  private String validateAndGetJobSetId(ArmadaComputer computer) throws IOException {
    String jobSetId = computer.getArmadaJobSetId();
    if (jobSetId == null || jobSetId.isEmpty()) {
      throw new IOException(String.format("Invalid configuration for 'jobSetId' with value '%s': No job set ID available for slave: %s",
          jobSetId, nodeName));
    }
    return jobSetId;
  }

  private ArmadaSlave getArmadaSlave() throws IOException, InterruptedException {
    Node node = context.get(Node.class);
    if (!(node instanceof ArmadaSlave)) {
      throw new AbortException(
          String.format("Node is not an Armada node: %s",
              node != null ? node.getNodeName() : null));
    }
    return (ArmadaSlave) node;
  }

  @Override
  public void close() {
    if (client != null) {
      try {
        client.close();
        LOGGER.fine("Closed KubernetesClient for pod: " + podName);
      } catch (Exception e) {
        LOGGER.warning("Failed to close KubernetesClient: " + e.getMessage());
        // Don't throw in close()
      }
    }
  }
}
