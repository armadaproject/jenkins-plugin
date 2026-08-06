package io.armadaproject.jenkins.plugin;

import hudson.slaves.SlaveComputer;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * Enriches a Pod specification with Jenkins agent configuration. Creates a defensive copy to avoid
 * mutating the input Pod. Mimics the behavior of PodTemplateBuilder from the Kubernetes plugin.
 */
public class PodEnricher {

  private static final Logger LOGGER = Logger.getLogger(PodEnricher.class.getName());

  private final ArmadaCloud cloud;
  private final ArmadaSlave slave;
  private final Pod inputPod;

  public PodEnricher(ArmadaCloud cloud, ArmadaSlave slave, Pod pod) {
    this.cloud = cloud;
    this.slave = slave;
    this.inputPod = pod;
  }

  /**
   * Enriches the pod by ensuring JNLP agent container exists with proper configuration. Creates a
   * defensive copy of the input pod to avoid mutation. Behavior matches Kubernetes plugin's
   * PodTemplateBuilder.
   *
   * @return a new enriched pod (does not mutate the input)
   */
  public Pod enrich() {
    // Create defensive copy to avoid mutating input
    Pod pod = new PodBuilder(inputPod).build();

    ensureWorkspaceVolume(pod);
    enrichAgentContainer(pod);
    return pod;
  }

  /**
   * Ensures workspace volume exists in the pod.
   *
   * @param pod the pod to modify
   */
  private void ensureWorkspaceVolume(Pod pod) {
    if (pod.getSpec().getVolumes() == null) {
      pod.getSpec().setVolumes(new ArrayList<>());
    }

    boolean hasWorkspaceVolume = pod.getSpec().getVolumes().stream()
        .anyMatch(v -> ArmadaPluginConfig.WORKSPACE_VOLUME_NAME.equals(v.getName()));

    if (!hasWorkspaceVolume) {
      Volume workspaceVolume = new VolumeBuilder()
          .withName(ArmadaPluginConfig.WORKSPACE_VOLUME_NAME)
          .withNewEmptyDir()
          .endEmptyDir()
          .build();
      pod.getSpec().getVolumes().add(workspaceVolume);
      LOGGER.fine("Added workspace volume to pod");
    }
  }

  /**
   * Enriches the agent container following K8s plugin logic: 1. If jnlp container doesn't exist,
   * create minimal one 2. Always enrich with Jenkins env vars (user env vars take precedence) 3.
   * Set default image if not specified 4. Set default resources if not specified
   *
   * @param pod the pod to modify
   */
  private void enrichAgentContainer(Pod pod) {
    if (pod.getSpec().getContainers() == null) {
      pod.getSpec().setContainers(new ArrayList<>());
    }

    // Find or create jnlp container
    Optional<Container> agentOpt = pod.getSpec().getContainers().stream()
        .filter(c -> ArmadaPluginConfig.JNLP_CONTAINER_NAME.equals(c.getName()))
        .findFirst();

    Container agentContainer;
    if (agentOpt.isEmpty()) {
      // Create minimal jnlp container with workspace volume mount
      LOGGER.fine("No jnlp container found, creating one");
      agentContainer = new ContainerBuilder()
          .withName(ArmadaPluginConfig.JNLP_CONTAINER_NAME)
          .withVolumeMounts(new VolumeMountBuilder()
              .withName(ArmadaPluginConfig.WORKSPACE_VOLUME_NAME)
              .withMountPath(ArmadaPluginConfig.DEFAULT_WORKING_DIR)
              .build())
          .build();
      pod.getSpec().getContainers().add(agentContainer);
    } else {
      agentContainer = agentOpt.get();
      LOGGER.fine("Found existing jnlp container, enriching it");
    }

    // Get working directory from agent container (or use default)
    String workingDir = agentContainer.getWorkingDir();
    if (workingDir == null) {
      workingDir = ArmadaPluginConfig.DEFAULT_WORKING_DIR;
    }

    // Propagate working dir to other containers if they don't have one
    final String finalWorkingDir = workingDir;
    pod.getSpec().getContainers().stream()
        .filter(c -> c.getWorkingDir() == null)
        .forEach(c -> c.setWorkingDir(finalWorkingDir));

    // Ensure ALL containers have the workspace volume mounted
    // (not just the JNLP container) so they can access the shared workspace
    pod.getSpec().getContainers().forEach(c -> ensureWorkspaceVolumeMount(c, finalWorkingDir));

    // Set default image if blank
    if (StringUtils.isBlank(agentContainer.getImage())) {
      agentContainer.setImage(ArmadaPluginConfig.DEFAULT_AGENT_IMAGE);
      LOGGER.fine("Set default agent image: " + ArmadaPluginConfig.DEFAULT_AGENT_IMAGE);
    }

    // Build environment variables with proper override order
    Map<String, EnvVar> envVars = new HashMap<>();

    // 1. Start with Jenkins agent env vars
    envVars.putAll(buildAgentEnvVars(workingDir));

    // 2. Let existing container env vars override
    if (agentContainer.getEnv() != null) {
      agentContainer.getEnv().forEach(env -> envVars.put(env.getName(), env));
    }

    // Always set the merged env vars
    agentContainer.setEnv(new ArrayList<>(envVars.values()));

    // Set default resources if not specified
    if (agentContainer.getResources() == null) {
      Map<String, Quantity> requests = new HashMap<>();
      requests.put("cpu", new Quantity(ArmadaPluginConfig.DEFAULT_JNLP_CONTAINER_CPU_REQUEST));
      requests.put("memory",
          new Quantity(ArmadaPluginConfig.DEFAULT_JNLP_CONTAINER_MEMORY_REQUEST));

      Map<String, Quantity> limits = new HashMap<>();
      limits.put("cpu", new Quantity(ArmadaPluginConfig.DEFAULT_JNLP_CONTAINER_CPU_LIMIT));
      limits.put("memory", new Quantity(ArmadaPluginConfig.DEFAULT_JNLP_CONTAINER_MEMORY_LIMIT));

      agentContainer.setResources(new ResourceRequirementsBuilder()
          .withRequests(requests)
          .withLimits(limits)
          .build());
    }
  }

  /**
   * Ensures the given container has the workspace volume mounted. This is required for all
   * containers to access the shared workspace directory.
   */
  private void ensureWorkspaceVolumeMount(Container container, String workingDir) {
    if (container.getVolumeMounts() == null) {
      container.setVolumeMounts(new ArrayList<>());
    }

    // Use noneMatch for cleaner logic - if no mount with this name exists, add it
    if (container.getVolumeMounts().stream()
        .noneMatch(vm -> ArmadaPluginConfig.WORKSPACE_VOLUME_NAME.equals(vm.getName()))) {
      container.getVolumeMounts().add(
          new VolumeMountBuilder()
              .withName(ArmadaPluginConfig.WORKSPACE_VOLUME_NAME)
              .withMountPath(workingDir)
              .build());  // readOnly defaults to false
      LOGGER.fine("Added workspace volume mount to container: " + container.getName());
    }
  }

  /**
   * Builds the environment variables required for JNLP agent to connect to Jenkins. Matches the
   * agentEnvVars() method from PodTemplateBuilder.
   */
  private Map<String, EnvVar> buildAgentEnvVars(String workingDir) {
    Map<String, EnvVar> envVars = new HashMap<>();

    SlaveComputer computer = slave.getComputer();
    if (computer != null) {
      // JENKINS_SECRET is sourced from a per-agent Kubernetes Secret created by
      // ArmadaLauncher after the pod is scheduled, so the HMAC never appears in the
      // submitted PodSpec where anyone with `get pods` could read it.
      envVars.put(ArmadaPluginConfig.JENKINS_SECRET_ENV, buildJnlpSecretEnvVar(computer.getName()));

      // Agent name (backwards compat)
      envVars.put(ArmadaPluginConfig.JENKINS_NAME_ENV,
          new EnvVar(ArmadaPluginConfig.JENKINS_NAME_ENV, computer.getName(), null));
      envVars.put(ArmadaPluginConfig.JENKINS_AGENT_NAME_ENV,
          new EnvVar(ArmadaPluginConfig.JENKINS_AGENT_NAME_ENV, computer.getName(), null));
    } else {
      LOGGER.log(Level.WARNING, "Computer is null for agent: {0}", slave.getNodeName());
    }

    // Working directory
    envVars.put(ArmadaPluginConfig.JENKINS_AGENT_WORKDIR_ENV,
        new EnvVar(ArmadaPluginConfig.JENKINS_AGENT_WORKDIR_ENV, workingDir, null));

    // Jenkins URL and connection settings
    String jenkinsUrl = cloud.getJenkinsUrl();
    if (StringUtils.isNotBlank(jenkinsUrl)) {
      if (!jenkinsUrl.endsWith("/")) {
        jenkinsUrl += "/";
      }
      envVars.put(ArmadaPluginConfig.JENKINS_URL_ENV,
          new EnvVar(ArmadaPluginConfig.JENKINS_URL_ENV, jenkinsUrl, null));
    } else {
      LOGGER.log(Level.WARNING, "Jenkins URL is not configured in Armada cloud: {0}", cloud.name);
    }

    // Remoting options
    envVars.put(ArmadaPluginConfig.REMOTING_OPTS_ENV,
        new EnvVar(ArmadaPluginConfig.REMOTING_OPTS_ENV,
            "-noReconnectAfter " + ArmadaPluginConfig.NO_RECONNECT_AFTER_TIMEOUT, null));

    return envVars;
  }

  /**
   * Builds an EnvVar that resolves JENKINS_SECRET from the per-agent Kubernetes Secret
   * (created by ArmadaLauncher post-schedule). The kubelet retries env resolution on backoff,
   * so the pod can be submitted before the Secret exists.
   */
  static EnvVar buildJnlpSecretEnvVar(String agentName) {
    return new EnvVarBuilder()
        .withName(ArmadaPluginConfig.JENKINS_SECRET_ENV)
        .withValueFrom(new EnvVarSourceBuilder()
            .withSecretKeyRef(new SecretKeySelectorBuilder()
                .withName(agentName + ArmadaPluginConfig.JNLP_SECRET_NAME_SUFFIX)
                .withKey(ArmadaPluginConfig.JNLP_SECRET_KEY)
                .withOptional(false)
                .build())
            .build())
        .build();
  }
}
