package io.armadaproject.jenkins.plugin.job;

import api.SubmitOuterClass.JobSubmitRequest;
import api.SubmitOuterClass.JobSubmitRequest.Builder;
import api.SubmitOuterClass.JobSubmitRequestItem;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import k8s.io.api.core.v1.Generated.AWSElasticBlockStoreVolumeSource;
import k8s.io.api.core.v1.Generated.Affinity;
import k8s.io.api.core.v1.Generated.Capabilities;
import k8s.io.api.core.v1.Generated.ConfigMapEnvSource;
import k8s.io.api.core.v1.Generated.ConfigMapKeySelector;
import k8s.io.api.core.v1.Generated.Container;
import k8s.io.api.core.v1.Generated.ContainerPort;
import k8s.io.api.core.v1.Generated.ContainerResizePolicy;
import k8s.io.api.core.v1.Generated.EmptyDirVolumeSource;
import k8s.io.api.core.v1.Generated.EnvFromSource;
import k8s.io.api.core.v1.Generated.EnvVar;
import k8s.io.api.core.v1.Generated.EnvVarSource;
import k8s.io.api.core.v1.Generated.EphemeralContainer;
import k8s.io.api.core.v1.Generated.EphemeralContainerCommon;
import k8s.io.api.core.v1.Generated.ExecAction;
import k8s.io.api.core.v1.Generated.FlexVolumeSource;
import k8s.io.api.core.v1.Generated.GCEPersistentDiskVolumeSource;
import k8s.io.api.core.v1.Generated.GRPCAction;
import k8s.io.api.core.v1.Generated.GitRepoVolumeSource;
import k8s.io.api.core.v1.Generated.GlusterfsVolumeSource;
import k8s.io.api.core.v1.Generated.HTTPGetAction;
import k8s.io.api.core.v1.Generated.HTTPHeader;
import k8s.io.api.core.v1.Generated.HostAlias;
import k8s.io.api.core.v1.Generated.HostPathVolumeSource;
import k8s.io.api.core.v1.Generated.ISCSIVolumeSource;
import k8s.io.api.core.v1.Generated.KeyToPath;
import k8s.io.api.core.v1.Generated.Lifecycle;
import k8s.io.api.core.v1.Generated.LifecycleHandler;
import k8s.io.api.core.v1.Generated.LocalObjectReference;
import k8s.io.api.core.v1.Generated.NFSVolumeSource;
import k8s.io.api.core.v1.Generated.NodeAffinity;
import k8s.io.api.core.v1.Generated.NodeSelector;
import k8s.io.api.core.v1.Generated.NodeSelectorRequirement;
import k8s.io.api.core.v1.Generated.NodeSelectorTerm;
import k8s.io.api.core.v1.Generated.ObjectFieldSelector;
import k8s.io.api.core.v1.Generated.PersistentVolumeClaimVolumeSource;
import k8s.io.api.core.v1.Generated.PodAffinity;
import k8s.io.api.core.v1.Generated.PodAffinityTerm;
import k8s.io.api.core.v1.Generated.PodAntiAffinity;
import k8s.io.api.core.v1.Generated.PodDNSConfig;
import k8s.io.api.core.v1.Generated.PodDNSConfigOption;
import k8s.io.api.core.v1.Generated.PodOS;
import k8s.io.api.core.v1.Generated.PodReadinessGate;
import k8s.io.api.core.v1.Generated.PodResourceClaim;
import k8s.io.api.core.v1.Generated.PodSchedulingGate;
import k8s.io.api.core.v1.Generated.PodSecurityContext;
import k8s.io.api.core.v1.Generated.PodSpec;
import k8s.io.api.core.v1.Generated.PreferredSchedulingTerm;
import k8s.io.api.core.v1.Generated.Probe;
import k8s.io.api.core.v1.Generated.ProbeHandler;
import k8s.io.api.core.v1.Generated.RBDVolumeSource;
import k8s.io.api.core.v1.Generated.ResourceFieldSelector;
import k8s.io.api.core.v1.Generated.ResourceRequirements;
import k8s.io.api.core.v1.Generated.SELinuxOptions;
import k8s.io.api.core.v1.Generated.SeccompProfile;
import k8s.io.api.core.v1.Generated.SecretEnvSource;
import k8s.io.api.core.v1.Generated.SecretKeySelector;
import k8s.io.api.core.v1.Generated.SecretVolumeSource;
import k8s.io.api.core.v1.Generated.SecurityContext;
import k8s.io.api.core.v1.Generated.SleepAction;
import k8s.io.api.core.v1.Generated.Sysctl;
import k8s.io.api.core.v1.Generated.TCPSocketAction;
import k8s.io.api.core.v1.Generated.Toleration;
import k8s.io.api.core.v1.Generated.TopologySpreadConstraint;
import k8s.io.api.core.v1.Generated.Volume;
import k8s.io.api.core.v1.Generated.VolumeDevice;
import k8s.io.api.core.v1.Generated.VolumeMount;
import k8s.io.api.core.v1.Generated.VolumeSource;
import k8s.io.api.core.v1.Generated.WeightedPodAffinityTerm;
import k8s.io.api.core.v1.Generated.WindowsSecurityContextOptions;
import k8s.io.apimachinery.pkg.api.resource.Generated.Quantity;
import k8s.io.apimachinery.pkg.apis.meta.v1.Generated.LabelSelector;
import k8s.io.apimachinery.pkg.apis.meta.v1.Generated.LabelSelectorRequirement;
import k8s.io.apimachinery.pkg.util.intstr.Generated.IntOrString;

public class ArmadaMapper {

  public final String queue;
  public final String namespace;
  public final String jobSetId;
  public final Pod pod;

  public ArmadaMapper(String queue, String namespace, String jobSetId, Pod pod) {
    this.queue = queue;
    this.namespace = namespace;
    this.jobSetId = jobSetId;
    this.pod = pod;
  }

  public JobSubmitRequest createJobSubmitRequest() {
    Builder builder = JobSubmitRequest.newBuilder();

    builder
        .setQueue(queue)
        .setJobSetId(jobSetId)
        .addJobRequestItems(createJobRequestItems(pod));

    return builder.build();
  }

  public JobSubmitRequestItem createJobRequestItems(Pod pod) {
    // priority skipped
    // client_id skipped
    // required_node_labels deprecated
    // pod_spec deprecated
    // ingress skipped
    // services skipped
    // scheduler skipped
    JobSubmitRequestItem.Builder builder = JobSubmitRequestItem.newBuilder();
    ObjectMeta metadata = pod.getMetadata();

    builder.setNamespace(Objects.nonNull(namespace) ? namespace : "default");

    if (metadata != null) {
      if (!metadata.getLabels().isEmpty()) {
        builder.putAllLabels(metadata.getLabels());
      }
      if (!metadata.getAnnotations().isEmpty()) {
        builder.putAllAnnotations(metadata.getAnnotations());
      }
    }

    if (pod.getSpec() != null) {
      builder.addPodSpecs(mapPodSpec(pod));
    }

    return builder.build();
  }

  public PodSpec mapPodSpec(Pod pod) {
    // initContainers skipped
    PodSpec.Builder builder = PodSpec.newBuilder();

    if (Objects.isNull(pod.getSpec())) {
      return builder.build();
    }

    io.fabric8.kubernetes.api.model.PodSpec podSpec = pod.getSpec();

    if (Objects.nonNull(podSpec.getVolumes())) {
      builder.addAllVolumes(mapVolumes(podSpec.getVolumes()));
    }

    if (Objects.nonNull(podSpec.getContainers())) {
      builder.addAllContainers(mapContainers(podSpec.getContainers()));
    }

    // NOT USED
//    if (Objects.nonNull(podSpec.getEphemeralContainers())) {
//      builder.addAllEphemeralContainers(mapEphermalContainers(podSpec.getEphemeralContainers()));
//    }

    if (Objects.nonNull(podSpec.getRestartPolicy())) {
      builder.setRestartPolicy(podSpec.getRestartPolicy());
    }

    if (Objects.nonNull(podSpec.getTerminationGracePeriodSeconds())) {
      builder.setTerminationGracePeriodSeconds(podSpec.getTerminationGracePeriodSeconds());
    }

    if (Objects.nonNull(podSpec.getActiveDeadlineSeconds())) {
      builder.setActiveDeadlineSeconds(podSpec.getActiveDeadlineSeconds());
    }

    if (Objects.nonNull(podSpec.getDnsPolicy())) {
      builder.setDnsPolicy(podSpec.getDnsPolicy());
    }

    if (Objects.nonNull(podSpec.getNodeSelector())) {
      builder.putAllNodeSelector(podSpec.getNodeSelector());
    }

    if (Objects.nonNull(podSpec.getServiceAccount())) {
      builder.setServiceAccount(podSpec.getServiceAccount());
    }

    if (Objects.nonNull(podSpec.getAutomountServiceAccountToken())) {
      builder.setAutomountServiceAccountToken(podSpec.getAutomountServiceAccountToken());
    }

    if (Objects.nonNull(podSpec.getNodeName())) {
      builder.setNodeName(podSpec.getNodeName());
    }

    if (Objects.nonNull(podSpec.getHostNetwork())) {
      builder.setHostNetwork(podSpec.getHostNetwork());
    }

    if (Objects.nonNull(podSpec.getHostPID())) {
      builder.setHostPID(podSpec.getHostPID());
    }

    if (Objects.nonNull(podSpec.getHostIPC())) {
      builder.setHostIPC(podSpec.getHostIPC());
    }

    if (Objects.nonNull(podSpec.getShareProcessNamespace())) {
      builder.setShareProcessNamespace(podSpec.getShareProcessNamespace());
    }

    if (Objects.nonNull(podSpec.getSecurityContext())) {
      builder.setSecurityContext(mapPodSecurityContext(podSpec.getSecurityContext()));
    }

    if (Objects.nonNull(podSpec.getImagePullSecrets())) {
      builder.addAllImagePullSecrets(mapLocalObjectReference(podSpec.getImagePullSecrets()));
    }

    if (Objects.nonNull(podSpec.getHostname())) {
      builder.setHostname(podSpec.getHostname());
    }

    if (Objects.nonNull(podSpec.getSubdomain())) {
      builder.setSubdomain(podSpec.getSubdomain());
    }

    if (Objects.nonNull(podSpec.getAffinity())) {
      builder.setAffinity(mapAffinity(podSpec.getAffinity()));
    }

    if (Objects.nonNull(podSpec.getSchedulerName())) {
      builder.setSchedulerName(podSpec.getSchedulerName());
    }

    if (Objects.nonNull(podSpec.getTolerations())) {
      builder.addAllTolerations(mapTolerations(podSpec.getTolerations()));
    }

    if (Objects.nonNull(podSpec.getHostAliases())) {
      builder.addAllHostAliases(mapHostAliases(podSpec.getHostAliases()));
    }

    if (Objects.nonNull(podSpec.getPriorityClassName())) {
      builder.setPriorityClassName(podSpec.getPriorityClassName());
    }

    if (Objects.nonNull(podSpec.getPriority())) {
      builder.setPriority(podSpec.getPriority());
    }

    if (Objects.nonNull(podSpec.getDnsConfig())) {
      builder.setDnsConfig(mapPodDnsConfig(podSpec.getDnsConfig()));
    }

    if (Objects.nonNull(podSpec.getReadinessGates())) {
      builder.addAllReadinessGates(mapPodReadinessGate(podSpec.getReadinessGates()));
    }

    if (Objects.nonNull(podSpec.getRuntimeClassName())) {
      builder.setRuntimeClassName(podSpec.getRuntimeClassName());
    }

    if (Objects.nonNull(podSpec.getEnableServiceLinks())) {
      builder.setEnableServiceLinks(podSpec.getEnableServiceLinks());
    }

    if (Objects.nonNull(podSpec.getPreemptionPolicy())) {
      builder.setPreemptionPolicy(podSpec.getPreemptionPolicy());
    }

    if (Objects.nonNull(podSpec.getOverhead())) {
      builder.putAllOverhead(mapOvearhead(podSpec.getOverhead()));
    }

    if (Objects.nonNull(podSpec.getTopologySpreadConstraints())) {
      builder.addAllTopologySpreadConstraints(mapTopologySpreadConstraints(
              podSpec.getTopologySpreadConstraints()));
    }

    if (Objects.nonNull(podSpec.getSetHostnameAsFQDN())) {
      builder.setSetHostnameAsFQDN(podSpec.getSetHostnameAsFQDN());
    }

    if (Objects.nonNull(podSpec.getOs())) {
      builder.setOs(mapPodOs(podSpec.getOs()));
    }

    if (Objects.nonNull(podSpec.getHostUsers())) {
      builder.setHostUsers(podSpec.getHostUsers());
    }

    if (Objects.nonNull(podSpec.getSchedulingGates())) {
      builder.addAllSchedulingGates(mapPodSchedulingGates(podSpec.getSchedulingGates()));
    }

    if (Objects.nonNull(podSpec.getResourceClaims())) {
      builder.addAllResourceClaims(mapPodResourceClaims(podSpec.getResourceClaims()));
    }

    return builder.build();
  }

  public Iterable<? extends PodResourceClaim> mapPodResourceClaims(
      List<io.fabric8.kubernetes.api.model.PodResourceClaim> resourceClaims) {
    return resourceClaims.stream()
        .map(r -> {
          PodResourceClaim.Builder builder = PodResourceClaim.newBuilder();

          if (Objects.nonNull(r.getName())) {
            builder.setName(r.getName());
          }

          // FIXME possible mismatch
          if (Objects.nonNull(r.getName())) {
            builder.setResourceClaimTemplateName(r.getName());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends PodSchedulingGate> mapPodSchedulingGates(
      List<io.fabric8.kubernetes.api.model.PodSchedulingGate> schedulingGates) {
    return schedulingGates.stream()
        .map(g -> {
          PodSchedulingGate.Builder builder = PodSchedulingGate.newBuilder();

          if (Objects.nonNull(g.getName())) {
            builder.setName(g.getName());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public PodOS mapPodOs(io.fabric8.kubernetes.api.model.PodOS os) {
    PodOS.Builder builder = PodOS.newBuilder();

    if (Objects.nonNull(os.getName())) {
      builder.setName(os.getName());
    }

    return builder.build();
  }

  public Iterable<? extends TopologySpreadConstraint> mapTopologySpreadConstraints(
      List<io.fabric8.kubernetes.api.model.TopologySpreadConstraint> topologySpreadConstraints) {
    return topologySpreadConstraints.stream()
        .map(c -> {
          TopologySpreadConstraint.Builder builder = TopologySpreadConstraint.newBuilder();

          if (Objects.nonNull(c.getMaxSkew())) {
            builder.setMaxSkew(c.getMaxSkew());
          }

          if (Objects.nonNull(c.getTopologyKey())) {
            builder.setTopologyKey(c.getTopologyKey());
          }

          if (Objects.nonNull(c.getWhenUnsatisfiable())) {
            builder.setWhenUnsatisfiable(c.getWhenUnsatisfiable());
          }

          if (Objects.nonNull(c.getLabelSelector())) {
            builder.setLabelSelector(mapLabelSelector(c.getLabelSelector()));
          }

          if (Objects.nonNull(c.getMinDomains())) {
            builder.setMinDomains(c.getMinDomains());
          }

          if (Objects.nonNull(c.getNodeAffinityPolicy())) {
            builder.setNodeAffinityPolicy(c.getNodeAffinityPolicy());
          }

          if (Objects.nonNull(c.getNodeTaintsPolicy())) {
            builder.setNodeTaintsPolicy(c.getNodeTaintsPolicy());
          }

          if (Objects.nonNull(c.getMatchLabelKeys())) {
            builder.addAllMatchLabelKeys(c.getMatchLabelKeys());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Map<String, Quantity> mapOvearhead(
      Map<String, io.fabric8.kubernetes.api.model.Quantity> overhead) {
    return overhead.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey,
            e -> {
              if (Objects.nonNull(e.getValue())) {
                return mapQuantity(e.getValue());
              }

              return Quantity.newBuilder().build();
            }));
  }

  public Iterable<? extends PodReadinessGate> mapPodReadinessGate(
      List<io.fabric8.kubernetes.api.model.PodReadinessGate> readinessGates) {
    return readinessGates.stream()
        .map(g -> {
          PodReadinessGate.Builder builder = PodReadinessGate.newBuilder();

          if (Objects.nonNull(g.getConditionType())) {
            builder.setConditionType(g.getConditionType());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public PodDNSConfig mapPodDnsConfig(io.fabric8.kubernetes.api.model.PodDNSConfig dnsConfig) {
    PodDNSConfig.Builder builder = PodDNSConfig.newBuilder();

    if (Objects.nonNull(dnsConfig.getNameservers())) {
      builder.addAllNameservers(dnsConfig.getNameservers());
    }

    if (Objects.nonNull(dnsConfig.getSearches())) {
      builder.addAllSearches(dnsConfig.getSearches());
    }

    if (Objects.nonNull(dnsConfig.getOptions())) {
      builder.addAllOptions(mapPodDNSConfigOption(dnsConfig.getOptions()));
    }

    return builder.build();
  }

  public Iterable<? extends PodDNSConfigOption> mapPodDNSConfigOption(
      List<io.fabric8.kubernetes.api.model.PodDNSConfigOption> options) {
    return options.stream()
        .map(o -> {
          PodDNSConfigOption.Builder builder = PodDNSConfigOption.newBuilder();

          if (Objects.nonNull(o.getName())) {
            builder.setName(o.getName());
          }

          if (Objects.nonNull(o.getValue())) {
            builder.setValue(o.getValue());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends HostAlias> mapHostAliases(
      List<io.fabric8.kubernetes.api.model.HostAlias> hostAliases) {
    return hostAliases.stream()
        .map(h -> {
          HostAlias.Builder builder = HostAlias.newBuilder();

          if (Objects.nonNull(h.getIp())) {
            builder.setIp(h.getIp());
          }

          if (Objects.nonNull(h.getHostnames())) {
            builder.addAllHostnames(h.getHostnames());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends Toleration> mapTolerations(
      List<io.fabric8.kubernetes.api.model.Toleration> tolerations) {
    return tolerations.stream()
        .map(t -> {
          Toleration.Builder builder = Toleration.newBuilder();
          if (Objects.nonNull(t.getKey())) {
            builder.setKey(t.getKey());
          }

          if (Objects.nonNull(t.getOperator())) {
            builder.setOperator(t.getOperator());
          }

          if (Objects.nonNull(t.getValue())) {
            builder.setValue(t.getValue());
          }

          if (Objects.nonNull(t.getEffect())) {
            builder.setEffect(t.getEffect());
          }

          if (Objects.nonNull(t.getTolerationSeconds())) {
            builder.setTolerationSeconds(t.getTolerationSeconds());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Affinity mapAffinity(io.fabric8.kubernetes.api.model.Affinity affinity) {
    Affinity.Builder builder = Affinity.newBuilder();

    if (Objects.nonNull(affinity.getNodeAffinity())) {
      builder.setNodeAffinity(mapNodeAffinity(affinity.getNodeAffinity()));
    }

    if (Objects.nonNull(affinity.getPodAffinity())) {
      builder.setPodAffinity(mapPodAffinity(affinity.getPodAffinity()));
    }

    if (Objects.nonNull(affinity.getPodAntiAffinity())) {
      builder.setPodAntiAffinity(mapPodAntiAffinity(affinity.getPodAntiAffinity()));
    }

    return builder.build();
  }

  public PodAntiAffinity mapPodAntiAffinity(
      io.fabric8.kubernetes.api.model.PodAntiAffinity podAntiAffinity) {
    PodAntiAffinity.Builder builder = PodAntiAffinity.newBuilder();

    if (Objects.nonNull(podAntiAffinity.getRequiredDuringSchedulingIgnoredDuringExecution())) {
      builder.addAllRequiredDuringSchedulingIgnoredDuringExecution(
          mapPodAffinityTerm(podAntiAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()));
    }

    if (Objects.nonNull(podAntiAffinity.getPreferredDuringSchedulingIgnoredDuringExecution())) {
      builder.addAllPreferredDuringSchedulingIgnoredDuringExecution(
          mapWeightedPodAffinityTerm(podAntiAffinity
              .getPreferredDuringSchedulingIgnoredDuringExecution()));
    }

    return builder.build();
  }

  public PodAffinity mapPodAffinity(io.fabric8.kubernetes.api.model.PodAffinity podAffinity) {
    PodAffinity.Builder builder = PodAffinity.newBuilder();

    if (Objects.nonNull(podAffinity.getRequiredDuringSchedulingIgnoredDuringExecution())) {
      builder.addAllRequiredDuringSchedulingIgnoredDuringExecution(
          mapPodAffinityTerm(podAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()));
    }

    if (Objects.nonNull(podAffinity.getPreferredDuringSchedulingIgnoredDuringExecution())) {
      builder.addAllPreferredDuringSchedulingIgnoredDuringExecution(
          mapWeightedPodAffinityTerm(podAffinity
              .getPreferredDuringSchedulingIgnoredDuringExecution()));
    }

    return builder
        .build();
  }

  public Iterable<? extends WeightedPodAffinityTerm> mapWeightedPodAffinityTerm(
      List<io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm> preferredDuringSchedulingIgnoredDuringExecution) {
    return preferredDuringSchedulingIgnoredDuringExecution.stream()
        .map(t -> {
          WeightedPodAffinityTerm.Builder builder = WeightedPodAffinityTerm.newBuilder();

          if (Objects.nonNull(t.getWeight())) {
            builder.setWeight(t.getWeight());
          }

          if (Objects.nonNull(t.getPodAffinityTerm())) {
            builder.setPodAffinityTerm(mapPodAffinityTerm(t.getPodAffinityTerm()));
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public PodAffinityTerm mapPodAffinityTerm(
      io.fabric8.kubernetes.api.model.PodAffinityTerm podAffinityTerm) {
    PodAffinityTerm.Builder builder = PodAffinityTerm.newBuilder();

    if (Objects.nonNull(podAffinityTerm.getLabelSelector())) {
      builder.setLabelSelector(mapLabelSelector(podAffinityTerm.getLabelSelector()));
    }

    if (Objects.nonNull(podAffinityTerm.getNamespaces())) {
      builder.addAllNamespaces(podAffinityTerm.getNamespaces());
    }

    if (Objects.nonNull(podAffinityTerm.getTopologyKey())) {
      builder.setTopologyKey(podAffinityTerm.getTopologyKey());
    }

    if (Objects.nonNull(podAffinityTerm.getNamespaceSelector())) {
      builder.setNamespaceSelector(mapLabelSelector(podAffinityTerm.getNamespaceSelector()));
    }

    if (Objects.nonNull(podAffinityTerm.getMatchLabelKeys())) {
      builder.addAllMatchLabelKeys(podAffinityTerm.getMatchLabelKeys());
    }

    if (Objects.nonNull(podAffinityTerm.getMismatchLabelKeys())) {
      builder.addAllMismatchLabelKeys(podAffinityTerm.getMismatchLabelKeys());
    }

    return builder.build();
  }

  public Iterable<? extends PodAffinityTerm> mapPodAffinityTerm(
      List<io.fabric8.kubernetes.api.model.PodAffinityTerm> requiredDuringSchedulingIgnoredDuringExecution) {
    return requiredDuringSchedulingIgnoredDuringExecution.stream()
        .map(t -> {
          PodAffinityTerm.Builder builder = PodAffinityTerm.newBuilder();

          if (Objects.nonNull(t.getLabelSelector())) {
            builder.setLabelSelector(mapLabelSelector(t.getLabelSelector()));
          }

          if (Objects.nonNull(t.getNamespaces())) {
            builder.addAllNamespaces(t.getNamespaces());
          }

          if (Objects.nonNull(t.getTopologyKey())) {
            builder.setTopologyKey(t.getTopologyKey());
          }

          if (Objects.nonNull(t.getNamespaceSelector())) {
            builder.setNamespaceSelector(mapLabelSelector(t.getNamespaceSelector()));
          }

          if (Objects.nonNull(t.getMatchLabelKeys())) {
            builder.addAllMatchLabelKeys(t.getMatchLabelKeys());
          }

          if (Objects.nonNull(t.getMismatchLabelKeys())) {
            builder.addAllMismatchLabelKeys(t.getMismatchLabelKeys());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public LabelSelector mapLabelSelector(
      io.fabric8.kubernetes.api.model.LabelSelector labelSelector) {
    LabelSelector.Builder builder = LabelSelector.newBuilder();

    if (Objects.nonNull(labelSelector.getMatchLabels())) {
      builder.putAllMatchLabels(labelSelector.getMatchLabels());
    }

    if (Objects.nonNull(labelSelector.getMatchExpressions())) {
      builder.addAllMatchExpressions(mapLabelSelectorRequirement(labelSelector.getMatchExpressions()));
    }

    return builder.build();
  }

  public Iterable<? extends LabelSelectorRequirement> mapLabelSelectorRequirement(
      List<io.fabric8.kubernetes.api.model.LabelSelectorRequirement> matchExpressions) {
    return matchExpressions.stream()
        .map(r -> {
          LabelSelectorRequirement.Builder builder = LabelSelectorRequirement.newBuilder();

          if (Objects.nonNull(r.getKey())) {
            builder.setKey(r.getKey());
          }

          if (Objects.nonNull(r.getOperator())) {
            builder.setOperator(r.getOperator());
          }

          if (Objects.nonNull(r.getValues())) {
            builder.addAllValues(r.getValues());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public NodeAffinity mapNodeAffinity(io.fabric8.kubernetes.api.model.NodeAffinity nodeAffinity) {
    NodeAffinity.Builder builder = NodeAffinity.newBuilder();

    if (Objects.nonNull(nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution())) {
      builder.setRequiredDuringSchedulingIgnoredDuringExecution(
          mapNodeSelector(nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()));
    }


    if (Objects.nonNull(nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution())) {
      builder.addAllPreferredDuringSchedulingIgnoredDuringExecution(
          mapPreferredSchedulingTerms(nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution()));
    }

    return builder.build();
  }

  public Iterable<? extends PreferredSchedulingTerm> mapPreferredSchedulingTerms(
      List<io.fabric8.kubernetes.api.model.PreferredSchedulingTerm> preferredDuringSchedulingIgnoredDuringExecution) {
    return preferredDuringSchedulingIgnoredDuringExecution.stream()
        .map(t -> {
          PreferredSchedulingTerm.Builder builder = PreferredSchedulingTerm.newBuilder();

          if (Objects.nonNull(t.getWeight())) {
            builder.setWeight(t.getWeight());
          }

          if (Objects.nonNull(t.getPreference())) {
            builder.setPreference(mapNodeSelector(t.getPreference()));
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public NodeSelectorTerm mapNodeSelector(
      io.fabric8.kubernetes.api.model.NodeSelectorTerm preference) {
    NodeSelectorTerm.Builder builder = NodeSelectorTerm.newBuilder();

    if (Objects.nonNull(preference.getMatchExpressions())) {
      builder.addAllMatchExpressions(mapNodeSelectorRequirement(preference.getMatchExpressions()));
    }

    if (Objects.nonNull(preference.getMatchFields())) {
      builder.addAllMatchFields(mapNodeSelectorRequirement(preference.getMatchFields()));
    }

    return builder.build();
  }

  public NodeSelector mapNodeSelector(
      io.fabric8.kubernetes.api.model.NodeSelector requiredDuringSchedulingIgnoredDuringExecution) {
    NodeSelector.Builder builder = NodeSelector.newBuilder();

    if (Objects.nonNull(requiredDuringSchedulingIgnoredDuringExecution.getNodeSelectorTerms())) {
      builder.addAllNodeSelectorTerms(mapNodeSelectorTerms(
          requiredDuringSchedulingIgnoredDuringExecution.getNodeSelectorTerms()));
    }

    return builder.build();
  }

  public Iterable<? extends NodeSelectorTerm> mapNodeSelectorTerms(
      List<io.fabric8.kubernetes.api.model.NodeSelectorTerm> nodeSelectorTerms) {
    return nodeSelectorTerms.stream()
        .map(t -> {
          NodeSelectorTerm.Builder builder = NodeSelectorTerm.newBuilder();

          if (Objects.nonNull(t.getMatchExpressions())) {
            builder.addAllMatchExpressions(mapNodeSelectorRequirement(t.getMatchExpressions()));
          }

          if (Objects.nonNull(t.getMatchFields())) {
            builder.addAllMatchFields(mapNodeSelectorRequirement(t.getMatchFields()));
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends NodeSelectorRequirement> mapNodeSelectorRequirement(
      List<io.fabric8.kubernetes.api.model.NodeSelectorRequirement> matchExpressions) {
    return matchExpressions.stream()
        .map(r -> {
          NodeSelectorRequirement.Builder builder = NodeSelectorRequirement.newBuilder();

          if (Objects.nonNull(r.getKey())) {
            builder.setKey(r.getKey());
          }

          if (Objects.nonNull(r.getOperator())) {
            builder.setOperator(r.getOperator());
          }

          if (Objects.nonNull(r.getValues())) {
            builder.addAllValues(r.getValues());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends LocalObjectReference> mapLocalObjectReference(
      List<io.fabric8.kubernetes.api.model.LocalObjectReference> imagePullSecrets) {
    return imagePullSecrets.stream()
        .map(r -> {
          LocalObjectReference.Builder builder = LocalObjectReference.newBuilder();

          if (Objects.nonNull(r.getName())) {
            builder.setName(r.getName());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public PodSecurityContext mapPodSecurityContext(
      io.fabric8.kubernetes.api.model.PodSecurityContext securityContext) {
    PodSecurityContext.Builder builder = PodSecurityContext.newBuilder();

    if (Objects.nonNull(securityContext.getSeLinuxOptions())) {
      builder.setSeLinuxOptions(mapSeLinuxOptions(securityContext.getSeLinuxOptions()));
    }

    if (Objects.nonNull(securityContext.getWindowsOptions())) {
      builder.setWindowsOptions(mapWindowsOptions(securityContext.getWindowsOptions()));
    }

    if (Objects.nonNull(securityContext.getRunAsUser())) {
      builder.setRunAsUser(securityContext.getRunAsUser());
    }

    if (Objects.nonNull(securityContext.getRunAsGroup())) {
      builder.setRunAsGroup(securityContext.getRunAsGroup());
    }

    if (Objects.nonNull(securityContext.getRunAsNonRoot())) {
      builder.setRunAsNonRoot(securityContext.getRunAsNonRoot());
    }

    if (Objects.nonNull(securityContext.getSupplementalGroups())) {
      builder.addAllSupplementalGroups(securityContext.getSupplementalGroups());
    }

    if (Objects.nonNull(securityContext.getFsGroup())) {
      builder.setFsGroup(securityContext.getFsGroup());
    }

    if (Objects.nonNull(securityContext.getSysctls())) {
      builder.addAllSysctls(mapSysctls(securityContext.getSysctls()));
    }

    if (Objects.nonNull(securityContext.getFsGroupChangePolicy())) {
      builder.setFsGroupChangePolicy(securityContext.getFsGroupChangePolicy());
    }

    if (Objects.nonNull(securityContext.getSeccompProfile())) {
      builder.setSeccompProfile(mapSeccompProfile(securityContext.getSeccompProfile()));
    }

    // FIXME mismatch
    // .setAppArmorProfile()
    // .setSupplementalGroupsPolicy()
    return builder.build();
  }

  public Iterable<? extends Sysctl> mapSysctls(
      List<io.fabric8.kubernetes.api.model.Sysctl> sysctls) {
    return sysctls.stream()
        .map(s -> {
          Sysctl.Builder builder = Sysctl.newBuilder();

          if (Objects.nonNull(s.getName())) {
            builder.setName(s.getName());
          }

          if (Objects.nonNull(s.getValue())) {
            builder.setValue(s.getValue());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public Iterable<? extends EphemeralContainer> mapEphermalContainers(
      List<io.fabric8.kubernetes.api.model.EphemeralContainer> ephemeralContainers) {
    return ephemeralContainers
        .stream()
        .map(c -> {
          EphemeralContainer.Builder ecBuilder = EphemeralContainer.newBuilder();

          EphemeralContainerCommon.Builder epcBuilder = EphemeralContainerCommon.newBuilder();
          if (Objects.nonNull(c.getName())) {
            epcBuilder.setName(c.getName());
          }

          if (Objects.nonNull(c.getImage())) {
            epcBuilder.setImage(c.getImage());
          }

          if (Objects.nonNull(c.getCommand())) {
            epcBuilder.addAllCommand(c.getCommand());
          }

          if (Objects.nonNull(c.getArgs())) {
            epcBuilder.addAllArgs(c.getArgs());
          }

          if (Objects.nonNull(c.getWorkingDir())) {
            epcBuilder.setWorkingDir(c.getWorkingDir());
          }

          if (Objects.nonNull(c.getPorts())) {
            epcBuilder.addAllPorts(mapContainerPorts(c.getPorts()));
          }

          if (Objects.nonNull(c.getEnvFrom())) {
            epcBuilder.addAllEnvFrom(mapEnvFromSource(c.getEnvFrom()));
          }

          if (Objects.nonNull(c.getEnv())) {
            epcBuilder.addAllEnv(mapEnvVars(c.getEnv()));
          }

          if (Objects.nonNull(c.getResources())) {
            epcBuilder.setResources(mapResourceRequirements(c.getResources()));
          }

          if (Objects.nonNull(c.getResizePolicy())) {
            epcBuilder.addAllResizePolicy(mapContainerResizePolicy(c.getResizePolicy()));
          }

          if (Objects.nonNull(c.getRestartPolicy())) {
            epcBuilder.setRestartPolicy(c.getRestartPolicy());
          }

          if (Objects.nonNull(c.getVolumeMounts())) {
            epcBuilder.addAllVolumeMounts(mapVolumeMounts(c.getVolumeMounts()));
          }

          if (Objects.nonNull(c.getVolumeDevices())) {
            epcBuilder.addAllVolumeDevices(mapVolumeDevices(c.getVolumeDevices()));
          }

          if (Objects.nonNull(c.getLivenessProbe())) {
            epcBuilder.setLivenessProbe(mapProbe(c.getLivenessProbe()));
          }

          if (Objects.nonNull(c.getReadinessProbe())) {
            epcBuilder.setReadinessProbe(mapProbe(c.getReadinessProbe()));
          }

          if (Objects.nonNull(c.getStartupProbe())) {
            epcBuilder.setStartupProbe(mapProbe(c.getStartupProbe()));
          }

          if (Objects.nonNull(c.getLifecycle())) {
            epcBuilder.setLifecycle(mapLifecycle(c.getLifecycle()));
          }

          if (Objects.nonNull(c.getTerminationMessagePath())) {
            epcBuilder.setTerminationMessagePath(c.getTerminationMessagePath());
          }

          if (Objects.nonNull(c.getTerminationMessagePolicy())) {
            epcBuilder.setTerminationMessagePolicy(c.getTerminationMessagePolicy());
          }

          if (Objects.nonNull(c.getImagePullPolicy())) {
            epcBuilder.setImagePullPolicy(c.getImagePullPolicy());
          }

          if (Objects.nonNull(c.getSecurityContext())) {
            epcBuilder.setSecurityContext(mapSecurityContext(c.getSecurityContext()));
          }

          if (Objects.nonNull(c.getStdin())) {
            epcBuilder.setStdin(c.getStdin());
          }

          if (Objects.nonNull(c.getStdinOnce())) {
            epcBuilder.setStdinOnce(c.getStdinOnce());
          }

          if (Objects.nonNull(c.getTty())) {
            epcBuilder.setTty(c.getTty());
          }

          ecBuilder.setEphemeralContainerCommon(epcBuilder.build());

          if (Objects.nonNull(c.getTargetContainerName())) {
            ecBuilder.setTargetContainerName(c.getTargetContainerName());
          }

          return ecBuilder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends Container> mapContainers(
      List<io.fabric8.kubernetes.api.model.Container> containers) {
    return containers
        .stream()
        .map(c -> {
          Container.Builder builder = Container.newBuilder();

          if (Objects.nonNull(c.getName())) {
            builder.setName(c.getName());
          }

          if (Objects.nonNull(c.getImage())) {
            builder.setImage(c.getImage());
          }

          if (Objects.nonNull(c.getCommand())) {
            builder.addAllCommand(c.getCommand());
          }

          if (Objects.nonNull(c.getArgs())) {
            builder.addAllArgs(c.getArgs());
          }

          if (Objects.nonNull(c.getWorkingDir())) {
            builder.setWorkingDir(c.getWorkingDir());
          }

          if (Objects.nonNull(c.getPorts())) {
            builder.addAllPorts(mapContainerPorts(c.getPorts()));
          }

          if (Objects.nonNull(c.getEnvFrom())) {
            builder.addAllEnvFrom(mapEnvFromSource(c.getEnvFrom()));
          }

          if (Objects.nonNull(c.getEnv())) {
            builder.addAllEnv(mapEnvVars(c.getEnv()));
          }

          if (Objects.nonNull(c.getResources())) {
            builder.setResources(mapResourceRequirements(c.getResources()));
          }

          if (Objects.nonNull(c.getResizePolicy())) {
            builder.addAllResizePolicy(mapContainerResizePolicy(c.getResizePolicy()));
          }

          if (Objects.nonNull(c.getRestartPolicy())) {
            builder.setRestartPolicy(c.getRestartPolicy());
          }

          if (Objects.nonNull(c.getVolumeMounts())) {
            builder.addAllVolumeMounts(mapVolumeMounts(c.getVolumeMounts()));
          }

          if (Objects.nonNull(c.getVolumeDevices())) {
            builder.addAllVolumeDevices(mapVolumeDevices(c.getVolumeDevices()));
          }

          if (Objects.nonNull(c.getLivenessProbe())) {
            builder.setLivenessProbe(mapProbe(c.getLivenessProbe()));
          }

          if (Objects.nonNull(c.getReadinessProbe())) {
            builder.setReadinessProbe(mapProbe(c.getReadinessProbe()));
          }

          if (Objects.nonNull(c.getStartupProbe())) {
            builder.setStartupProbe(mapProbe(c.getStartupProbe()));
          }

          if (Objects.nonNull(c.getLifecycle())) {
            builder.setLifecycle(mapLifecycle(c.getLifecycle()));
          }

          if (Objects.nonNull(c.getTerminationMessagePath())) {
            builder.setTerminationMessagePath(c.getTerminationMessagePath());
          }

          if (Objects.nonNull(c.getTerminationMessagePolicy())) {
            builder.setTerminationMessagePolicy(c.getTerminationMessagePolicy());
          }

          if (Objects.nonNull(c.getImagePullPolicy())) {
            builder.setImagePullPolicy(c.getImagePullPolicy());
          }

          if (Objects.nonNull(c.getSecurityContext())) {
            builder.setSecurityContext(mapSecurityContext(c.getSecurityContext()));
          }

          if (Objects.nonNull(c.getStdin())) {
            builder.setStdin(c.getStdin());
          }

          if (Objects.nonNull(c.getStdinOnce())) {
            builder.setStdinOnce(c.getStdinOnce());
          }

          if (Objects.nonNull(c.getTty())) {
            builder.setTty(c.getTty());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public SecurityContext mapSecurityContext(
      io.fabric8.kubernetes.api.model.SecurityContext securityContext) {
    SecurityContext.Builder builder = SecurityContext.newBuilder();

    if (Objects.nonNull(securityContext.getCapabilities())) {
      builder.setCapabilities(mapCapabilities(securityContext.getCapabilities()));
    }

    if (Objects.nonNull(securityContext.getPrivileged())) {
      builder.setPrivileged(securityContext.getPrivileged());
    }

    if (Objects.nonNull(securityContext.getSeLinuxOptions())) {
      builder.setSeLinuxOptions(mapSeLinuxOptions(securityContext.getSeLinuxOptions()));
    }

    if (Objects.nonNull(securityContext.getWindowsOptions())) {
      builder.setWindowsOptions(mapWindowsOptions(securityContext.getWindowsOptions()));
    }

    if (Objects.nonNull(securityContext.getRunAsUser())) {
      builder.setRunAsUser(securityContext.getRunAsUser());
    }

    if (Objects.nonNull(securityContext.getRunAsGroup())) {
      builder.setRunAsGroup(securityContext.getRunAsGroup());
    }

    if (Objects.nonNull(securityContext.getRunAsNonRoot())) {
      builder.setRunAsNonRoot(securityContext.getRunAsNonRoot());
    }

    if (Objects.nonNull(securityContext.getReadOnlyRootFilesystem())) {
      builder.setReadOnlyRootFilesystem(securityContext.getReadOnlyRootFilesystem());
    }

    if (Objects.nonNull(securityContext.getAllowPrivilegeEscalation())) {
      builder.setAllowPrivilegeEscalation(securityContext.getAllowPrivilegeEscalation());
    }

    if (Objects.nonNull(securityContext.getProcMount())) {
      builder.setProcMount(securityContext.getProcMount());
    }

    if (Objects.nonNull(securityContext.getSeccompProfile())) {
      builder.setSeccompProfile(mapSeccompProfile(securityContext.getSeccompProfile()));
    }

    // FIXME mismatch
    // .setAppArmorProfile()
    return builder.build();
  }

  public SeccompProfile mapSeccompProfile(
      io.fabric8.kubernetes.api.model.SeccompProfile seccompProfile) {
    SeccompProfile.Builder builder = SeccompProfile.newBuilder();

    if (Objects.nonNull(seccompProfile.getType())) {
      builder.setType(seccompProfile.getType());
    }

    if (Objects.nonNull(seccompProfile.getLocalhostProfile())) {
      builder.setLocalhostProfile(seccompProfile.getLocalhostProfile());
    }

    return builder.build();
  }

  public WindowsSecurityContextOptions mapWindowsOptions(
      io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions windowsOptions) {
    WindowsSecurityContextOptions.Builder builder = WindowsSecurityContextOptions.newBuilder();

    if (Objects.nonNull(windowsOptions.getGmsaCredentialSpec())) {
      builder.setGmsaCredentialSpec(windowsOptions.getGmsaCredentialSpec());
    }

    if (Objects.nonNull(windowsOptions.getGmsaCredentialSpecName())) {
      builder.setGmsaCredentialSpecName(windowsOptions.getGmsaCredentialSpecName());
    }

    if (Objects.nonNull(windowsOptions.getRunAsUserName())) {
      builder.setRunAsUserName(windowsOptions.getRunAsUserName());
    }

    if (Objects.nonNull(windowsOptions.getHostProcess())) {
      builder.setHostProcess(windowsOptions.getHostProcess());
    }

    return builder.build();
  }

  public SELinuxOptions mapSeLinuxOptions(
      io.fabric8.kubernetes.api.model.SELinuxOptions seLinuxOptions) {
    SELinuxOptions.Builder builder = SELinuxOptions.newBuilder();

    if (Objects.nonNull(seLinuxOptions.getUser())) {
      builder.setUser(seLinuxOptions.getUser());
    }

    if (Objects.nonNull(seLinuxOptions.getRole())) {
      builder.setRole(seLinuxOptions.getRole());
    }

    if (Objects.nonNull(seLinuxOptions.getType())) {
      builder.setType(seLinuxOptions.getType());
    }

    if (Objects.nonNull(seLinuxOptions.getLevel())) {
      builder.setLevel(seLinuxOptions.getLevel());
    }

    return builder.build();
  }

  public Capabilities mapCapabilities(io.fabric8.kubernetes.api.model.Capabilities capabilities) {
    Capabilities.Builder builder = Capabilities.newBuilder();

    if (Objects.nonNull(capabilities.getDrop())) {
      builder.addAllDrop(capabilities.getDrop());
    }

    if (Objects.nonNull(capabilities.getAdd())) {
      builder.addAllAdd(capabilities.getAdd());
    }

    return builder.build();
  }

  public Lifecycle mapLifecycle(io.fabric8.kubernetes.api.model.Lifecycle lifecycle) {
    Lifecycle.Builder builder = Lifecycle.newBuilder();

    if (Objects.nonNull(lifecycle.getPostStart())) {
      builder.setPostStart(mapLifecycleHandler(lifecycle.getPostStart()));
    }

    if (Objects.nonNull(lifecycle.getPreStop())) {
      builder.setPreStop(mapLifecycleHandler(lifecycle.getPreStop()));
    }

    return builder.build();
  }

  public LifecycleHandler mapLifecycleHandler(
      io.fabric8.kubernetes.api.model.LifecycleHandler postStart) {
    LifecycleHandler.Builder builder = LifecycleHandler.newBuilder();

    if (Objects.nonNull(postStart.getExec())) {
      if (Objects.nonNull(postStart.getExec().getCommand())) {
        builder.setExec(
            ExecAction.newBuilder().addAllCommand(postStart.getExec().getCommand()).build());
      }
    }

    if (Objects.nonNull(postStart.getHttpGet())) {
      builder.setHttpGet(mapHttpGetAction(postStart.getHttpGet()));
    }

    if (Objects.nonNull(postStart.getTcpSocket())) {
      builder.setTcpSocket(mapTCPSocketAction(postStart.getTcpSocket()));
    }

    if (Objects.nonNull(postStart.getSleep())) {
      if (Objects.nonNull(postStart.getSleep().getSeconds())) {
        builder.setSleep(
            SleepAction.newBuilder().setSeconds(postStart.getSleep().getSeconds()).build());
      }
    }

    return builder.build();
  }

  public Probe mapProbe(io.fabric8.kubernetes.api.model.Probe livenessProbe) {
    Probe.Builder builder = Probe.newBuilder();

    // TODO might be issues because proble handler is always default if nothing else
    builder.setHandler(mapProbeHandler(livenessProbe));

    if (Objects.nonNull(livenessProbe.getInitialDelaySeconds())) {
      builder.setInitialDelaySeconds(livenessProbe.getInitialDelaySeconds());
    }

    if (Objects.nonNull(livenessProbe.getTimeoutSeconds())) {
      builder.setTimeoutSeconds(livenessProbe.getTimeoutSeconds());
    }

    if (Objects.nonNull(livenessProbe.getPeriodSeconds())) {
      builder.setPeriodSeconds(livenessProbe.getPeriodSeconds());
    }

    if (Objects.nonNull(livenessProbe.getSuccessThreshold())) {
      builder.setSuccessThreshold(livenessProbe.getSuccessThreshold());
    }

    if (Objects.nonNull(livenessProbe.getFailureThreshold())) {
      builder.setFailureThreshold(livenessProbe.getFailureThreshold());
    }

    if (Objects.nonNull(livenessProbe.getTerminationGracePeriodSeconds())) {
      builder.setTerminationGracePeriodSeconds(livenessProbe.getTerminationGracePeriodSeconds());
    }

    return builder.build();
  }

  public ExecAction mapExecAction(io.fabric8.kubernetes.api.model.ExecAction exec) {
    ExecAction.Builder builder = ExecAction.newBuilder();

    if (Objects.nonNull(exec.getCommand())) {
      builder.addAllCommand(exec.getCommand());
    }

    return builder.build();
  }

  public ProbeHandler mapProbeHandler(io.fabric8.kubernetes.api.model.Probe livenessProbe) {
    ProbeHandler.Builder builder = ProbeHandler.newBuilder();

    if (Objects.nonNull(livenessProbe.getExec())) {
      builder.setExec(mapExecAction(livenessProbe.getExec()));
    }

    if (Objects.nonNull(livenessProbe.getHttpGet())) {
      builder.setHttpGet(mapHttpGetAction(livenessProbe.getHttpGet()));
    }

    if (Objects.nonNull(livenessProbe.getTcpSocket())) {
      builder.setTcpSocket(mapTCPSocketAction(livenessProbe.getTcpSocket()));
    }

    if (Objects.nonNull(livenessProbe.getGrpc())) {
      builder.setGrpc(mapGRPCAction(livenessProbe.getGrpc()));
    }

    return builder.build();
  }

  public GRPCAction mapGRPCAction(io.fabric8.kubernetes.api.model.GRPCAction grpc) {
    GRPCAction.Builder builder = GRPCAction.newBuilder();

    if (Objects.nonNull(grpc.getPort())) {
      builder.setPort(grpc.getPort());
    }

    if (Objects.nonNull(grpc.getService())) {
      builder.setService(grpc.getService());
    }

    return builder.build();
  }

  public TCPSocketAction mapTCPSocketAction(
      io.fabric8.kubernetes.api.model.TCPSocketAction tcpSocket) {
    TCPSocketAction.Builder builder = TCPSocketAction.newBuilder();

    if (Objects.nonNull(tcpSocket.getPort())) {
      builder.setPort(mapIntOrString(tcpSocket.getPort()));
    }

    if (Objects.nonNull(tcpSocket.getHost())) {
      builder.setHost(tcpSocket.getHost());
    }

    return builder.build();
  }

  public HTTPGetAction mapHttpGetAction(io.fabric8.kubernetes.api.model.HTTPGetAction httpGet) {
    HTTPGetAction.Builder builder = HTTPGetAction.newBuilder();

    if (Objects.nonNull(httpGet.getPath())) {
      builder.setPath(httpGet.getPath());
    }

    if (Objects.nonNull(httpGet.getPort())) {
      builder.setPort(mapIntOrString(httpGet.getPort()));
    }

    if (Objects.nonNull(httpGet.getHost())) {
      builder.setHost(httpGet.getHost());
    }

    if (Objects.nonNull(httpGet.getScheme())) {
      builder.setScheme(httpGet.getScheme());
    }

    if (Objects.nonNull(httpGet.getHttpHeaders())) {
      builder.addAllHttpHeaders(mapHttpHeaders(httpGet.getHttpHeaders()));
    }

    return builder.build();
  }

  public Iterable<? extends HTTPHeader> mapHttpHeaders(
      List<io.fabric8.kubernetes.api.model.HTTPHeader> httpHeaders) {
    return httpHeaders.stream()
        .map(h -> {
          HTTPHeader.Builder builder = HTTPHeader.newBuilder();

          if (Objects.nonNull(h.getName())) {
            builder.setName(h.getName());
          }

          if (Objects.nonNull(h.getValue())) {
            builder.setValue(h.getValue());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public IntOrString mapIntOrString(io.fabric8.kubernetes.api.model.IntOrString port) {
    IntOrString.Builder builder = IntOrString.newBuilder();

    if (Objects.nonNull(port.getIntVal())) {
      builder.setIntVal(port.getIntVal());
    }

    if (Objects.nonNull(port.getStrVal())) {
      builder.setStrVal(port.getStrVal());
    }

    return builder.build();
  }

  public Iterable<? extends VolumeDevice> mapVolumeDevices(
      List<io.fabric8.kubernetes.api.model.VolumeDevice> volumeDevices) {
    return volumeDevices.stream()
        .map(d -> {
          VolumeDevice.Builder builder = VolumeDevice.newBuilder();

          if (Objects.nonNull(d.getName())) {
            builder.setName(d.getName());
          }

          if (Objects.nonNull(d.getDevicePath())) {
            builder.setDevicePath(d.getDevicePath());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends VolumeMount> mapVolumeMounts(
      List<io.fabric8.kubernetes.api.model.VolumeMount> volumeMounts) {
    // FIXME mismatch
    // .setRecursiveReadOnly()
    return volumeMounts.stream()
        .map(m -> {
          VolumeMount.Builder builder = VolumeMount.newBuilder();

          if (Objects.nonNull(m.getName())) {
            builder.setName(m.getName());
          }

          if (Objects.nonNull(m.getReadOnly())) {
            builder.setReadOnly(m.getReadOnly());
          }

          if (Objects.nonNull(m.getMountPath())) {
            builder.setMountPath(m.getMountPath());
          }

          if (Objects.nonNull(m.getSubPath())) {
            builder.setSubPath(m.getSubPath());
          }

          if (Objects.nonNull(m.getMountPropagation())) {
            builder.setMountPropagation(m.getMountPropagation());
          }

          if (Objects.nonNull(m.getSubPathExpr())) {
            builder.setSubPathExpr(m.getSubPathExpr());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends ContainerResizePolicy> mapContainerResizePolicy(
      List<io.fabric8.kubernetes.api.model.ContainerResizePolicy> resizePolicy) {
    return resizePolicy.stream()
        .map(p -> {
          ContainerResizePolicy.Builder builder = ContainerResizePolicy.newBuilder();

          if (Objects.nonNull(p.getResourceName())) {
            builder.setResourceName(p.getResourceName());
          }

          if (Objects.nonNull(p.getRestartPolicy())) {
            builder.setRestartPolicy(p.getRestartPolicy());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public ResourceRequirements mapResourceRequirements(
      io.fabric8.kubernetes.api.model.ResourceRequirements resources) {
    ResourceRequirements.Builder builder = ResourceRequirements.newBuilder();

    if (Objects.nonNull(resources.getLimits())) {
      builder.putAllLimits(mapResourceLimits(resources.getLimits()));
    }

    if (Objects.nonNull(resources.getRequests())) {
      builder.putAllRequests(mapResourceRequests(resources.getRequests()));
    }

    return builder.build();
  }

  public Quantity mapQuantity(io.fabric8.kubernetes.api.model.Quantity quantity) {
    Quantity.Builder builder = Quantity.newBuilder();

    if (Objects.nonNull(quantity.getAmount())) {
      StringBuilder sb = new StringBuilder();
      sb.append(quantity.getAmount());

      if (Objects.nonNull(quantity.getFormat())) {
        sb.append(quantity.getFormat());
      }

      builder.setString(sb.toString());
    }

    return builder.build();
  }

  public Map<String, Quantity> mapResourceRequests(
      Map<String, io.fabric8.kubernetes.api.model.Quantity> requests) {

    return requests.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> mapQuantity(e.getValue())));
  }

  public Map<String, Quantity> mapResourceLimits(
      Map<String, io.fabric8.kubernetes.api.model.Quantity> limits) {

    return limits.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> mapQuantity(e.getValue())));
  }

  public Iterable<? extends EnvFromSource> mapEnvFromSource(
      List<io.fabric8.kubernetes.api.model.EnvFromSource> envFrom) {
    return envFrom.stream()
        .map(e -> {
          EnvFromSource.Builder builder = EnvFromSource.newBuilder();

          if (Objects.nonNull(e.getConfigMapRef())) {
            builder.setConfigMapRef(mapConfigMapEnvSource(e.getConfigMapRef()));
          }

          if (Objects.nonNull(e.getSecretRef())) {
            builder.setSecretRef(mapSecretEnvSource(e.getSecretRef()));
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public SecretEnvSource mapSecretEnvSource(
      io.fabric8.kubernetes.api.model.SecretEnvSource secretRef) {
    SecretEnvSource.Builder builder = SecretEnvSource.newBuilder();

    if (Objects.nonNull(secretRef.getName())) {
      builder.setLocalObjectReference(mapLocalObjectReference(secretRef.getName()));
    }

    if (Objects.nonNull(secretRef.getOptional())) {
      builder.setOptional(secretRef.getOptional());
    }

    return builder.build();
  }

  public ConfigMapEnvSource mapConfigMapEnvSource(
      io.fabric8.kubernetes.api.model.ConfigMapEnvSource configMapRef) {
    ConfigMapEnvSource.Builder builder = ConfigMapEnvSource.newBuilder();

    if (Objects.nonNull(configMapRef.getName())) {
      builder.setLocalObjectReference(mapLocalObjectReference(configMapRef.getName()));
    }

    if (Objects.nonNull(configMapRef.getOptional())) {
      builder.setOptional(configMapRef.getOptional());
    }

    return builder
        .build();
  }

  public Iterable<? extends EnvVar> mapEnvVars(List<io.fabric8.kubernetes.api.model.EnvVar> env) {
    return env.stream()
        .map(e -> {
          EnvVar.Builder builder = EnvVar.newBuilder();

          if (Objects.nonNull(e.getName())) {
            builder.setName(e.getName());
          }

          if (Objects.nonNull(e.getValue())) {
            builder.setValue(e.getValue());
          }

          if (Objects.nonNull(e.getValueFrom())) {
            builder.setValueFrom(mapEnvVarSource(e.getValueFrom()));
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public EnvVarSource mapEnvVarSource(io.fabric8.kubernetes.api.model.EnvVarSource valueFrom) {
    EnvVarSource.Builder builder = EnvVarSource.newBuilder();

    if (Objects.nonNull(valueFrom.getFieldRef())) {
      builder.setFieldRef(mapObjectFieldSelector(valueFrom.getFieldRef()));
    }

    if (Objects.nonNull(valueFrom.getResourceFieldRef())) {
      builder.setResourceFieldRef(mapResourceFieldSelector(valueFrom.getResourceFieldRef()));
    }

    if (Objects.nonNull(valueFrom.getConfigMapKeyRef())) {
      builder.setConfigMapKeyRef(mapConfigMapKeySelector(valueFrom.getConfigMapKeyRef()));
    }

    if (Objects.nonNull(valueFrom.getSecretKeyRef())) {
      builder.setSecretKeyRef(mapSecretKeySelector(valueFrom.getSecretKeyRef()));
    }

    return builder.build();
  }

  public SecretKeySelector mapSecretKeySelector(
      io.fabric8.kubernetes.api.model.SecretKeySelector secretKeyRef) {
    SecretKeySelector.Builder builder = SecretKeySelector.newBuilder();

    if (Objects.nonNull(secretKeyRef.getName())) {
      builder.setLocalObjectReference(mapLocalObjectReference(secretKeyRef.getName()));
    }

    if (Objects.nonNull(secretKeyRef.getKey())) {
      builder.setKey(secretKeyRef.getKey());
    }

    if (Objects.nonNull(secretKeyRef.getOptional())) {
      builder.setOptional(secretKeyRef.getOptional());
    }

    return builder.build();
  }

  public ConfigMapKeySelector mapConfigMapKeySelector(
      io.fabric8.kubernetes.api.model.ConfigMapKeySelector configMapKeyRef) {
    ConfigMapKeySelector.Builder builder = ConfigMapKeySelector.newBuilder();

    if (Objects.nonNull(configMapKeyRef.getName())) {
      builder.setLocalObjectReference(mapLocalObjectReference(configMapKeyRef.getName()));
    }

    if (Objects.nonNull(configMapKeyRef.getKey())) {
      builder.setKey(configMapKeyRef.getKey());
    }

    if (Objects.nonNull(configMapKeyRef.getOptional())) {
      builder.setOptional(configMapKeyRef.getOptional());
    }

    return builder.build();
  }

  public LocalObjectReference mapLocalObjectReference(String name) {
    LocalObjectReference.Builder builder = LocalObjectReference.newBuilder();

    if (Objects.nonNull(name)) {
      builder.setName(name);
    }

    return builder.build();
  }

  public ResourceFieldSelector mapResourceFieldSelector(
      io.fabric8.kubernetes.api.model.ResourceFieldSelector resourceFieldRef) {
    ResourceFieldSelector.Builder builder = ResourceFieldSelector.newBuilder();

    if (Objects.nonNull(resourceFieldRef.getContainerName())) {
      builder.setContainerName(resourceFieldRef.getContainerName());
    }

    if (Objects.nonNull(resourceFieldRef.getResource())) {
      builder.setResource(resourceFieldRef.getResource());
    }

    if (Objects.nonNull(resourceFieldRef.getDivisor())) {
      builder.setDivisor(mapQuantity(resourceFieldRef.getDivisor()));
    }

    return builder.build();
  }

  public ObjectFieldSelector mapObjectFieldSelector(
      io.fabric8.kubernetes.api.model.ObjectFieldSelector fieldRef) {
    ObjectFieldSelector.Builder builder = ObjectFieldSelector.newBuilder();

    if (Objects.nonNull(fieldRef.getApiVersion())) {
      builder.setApiVersion(fieldRef.getApiVersion());
    }

    if (Objects.nonNull(fieldRef.getFieldPath())) {
      builder.setFieldPath(fieldRef.getFieldPath());
    }

    return builder.build();
  }

  public Iterable<? extends ContainerPort> mapContainerPorts(
      List<io.fabric8.kubernetes.api.model.ContainerPort> ports) {
    return ports.stream()
        .map(p -> {
          ContainerPort.Builder builder = ContainerPort.newBuilder();

          if (Objects.nonNull(p.getName())) {
            builder.setName(p.getName());
          }

          if (Objects.nonNull(p.getHostPort())) {
            builder.setHostPort(p.getHostPort());
          }

          if (Objects.nonNull(p.getContainerPort())) {
            builder.setContainerPort(p.getContainerPort());
          }

          if (Objects.nonNull(p.getProtocol())) {
            builder.setProtocol(p.getProtocol());
          }

          if (Objects.nonNull(p.getHostIP())) {
            builder.setHostIP(p.getHostIP());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  public Iterable<? extends Volume> mapVolumes(
      List<io.fabric8.kubernetes.api.model.Volume> volumes) {
    List<Volume> vols = new ArrayList<>();

    volumes.forEach(v -> {
      Volume.Builder builder = Volume.newBuilder();

      if (Objects.nonNull(v.getName())) {
      builder.setName(v.getName());
      }

      builder.setVolumeSource(mapVolumeSource(v));

      vols.add(builder.build());
    });

    return vols;
  }

  public VolumeSource mapVolumeSource(io.fabric8.kubernetes.api.model.Volume volume) {
    VolumeSource.Builder builder = VolumeSource.newBuilder();

    if (Objects.nonNull(volume.getEmptyDir())) {
      builder.setEmptyDir(mapEmptyDirVolumeSource(volume.getEmptyDir()));
    }

    if (Objects.nonNull(volume.getSecret())) {
      builder.setSecret(mapSecretVolumeSource(volume.getSecret()));
    }

//    builder
//        .setHostPath(mapHostPathVolumeSource(volume))
//        .setGcePersistentDisk(mapGcePersistentDiskVolumeSource(volume))
//        .setAwsElasticBlockStore(mapAwsElasticBlockStoreVolumeSource(volume))
//        .setGitRepo(mapGitRepoVolumeSource(volume))
//        .setSecret(mapSecretVolumeSource(volume))
//        .setNfs(mapNfsVolumeSource(volume))
//        .setIscsi(mapIscsiVolumeSource(volume))
//        .setGlusterfs(mapGlueterfs(volume))
//        .setPersistentVolumeClaim(mapPersistentVolumeClaim(volume))
//        .setRbd(mapRbdVolumeSource(volume))
//        .setFlexVolume(mapFlexVolume(volume));
    ;

    return builder.build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public FlexVolumeSource mapFlexVolume(io.fabric8.kubernetes.api.model.Volume volume) {
    return FlexVolumeSource.newBuilder()
        .setDriver(volume.getFlexVolume().getDriver())
        .setFsType(volume.getFlexVolume().getFsType())
        .setSecretRef(mapLocalObjectReference(volume.getFlexVolume().getSecretRef()))
        .setReadOnly(volume.getFlexVolume().getReadOnly())
        .putAllOptions(volume.getFlexVolume().getOptions())
        .build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public RBDVolumeSource mapRbdVolumeSource(io.fabric8.kubernetes.api.model.Volume volume) {
    return RBDVolumeSource.newBuilder()
        // FIXME
        // .setMonitors(volume.getRbd().getMonitors()) type mismatch
        .setImage(volume.getRbd().getImage())
        .setFsType(volume.getRbd().getFsType())
        .setPool(volume.getRbd().getPool())
        .setUser(volume.getRbd().getUser())
        .setKeyring(volume.getRbd().getKeyring())
        .setSecretRef(mapLocalObjectReference(volume.getRbd().getSecretRef()))
        .setReadOnly(volume.getRbd().getReadOnly())
        .build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public PersistentVolumeClaimVolumeSource mapPersistentVolumeClaim(
      io.fabric8.kubernetes.api.model.Volume volume) {
    return PersistentVolumeClaimVolumeSource.newBuilder()
        .setClaimName(volume.getPersistentVolumeClaim().getClaimName())
        .setReadOnly(volume.getPersistentVolumeClaim().getReadOnly())
        .build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public GlusterfsVolumeSource mapGlueterfs(io.fabric8.kubernetes.api.model.Volume volume) {
    return GlusterfsVolumeSource.newBuilder()
        .setEndpoints(volume.getGlusterfs().getEndpoints())
        .setPath(volume.getGlusterfs().getPath())
        .setReadOnly(volume.getGlusterfs().getReadOnly())
        .build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public ISCSIVolumeSource mapIscsiVolumeSource(io.fabric8.kubernetes.api.model.Volume volume) {
    return ISCSIVolumeSource.newBuilder()
        .setTargetPortal(volume.getIscsi().getTargetPortal())
        .setIqn(volume.getIscsi().getIqn())
        .setLun(volume.getIscsi().getLun())
        .setIscsiInterface(volume.getIscsi().getIscsiInterface())
        .setFsType(volume.getIscsi().getFsType())
        .setReadOnly(volume.getIscsi().getReadOnly())
        .addAllPortals(volume.getIscsi().getPortals())
        .setChapAuthDiscovery(volume.getIscsi().getChapAuthDiscovery())
        .setChapAuthSession(volume.getIscsi().getChapAuthSession())
        .setSecretRef(mapLocalObjectReference(volume.getIscsi().getSecretRef()))
        .setInitiatorName(volume.getIscsi().getInitiatorName())
        .build();
  }

  public LocalObjectReference mapLocalObjectReference(
      io.fabric8.kubernetes.api.model.LocalObjectReference secretRef) {
    LocalObjectReference.Builder builder = LocalObjectReference.newBuilder();

    if (Objects.nonNull(secretRef.getName())) {
      builder.setName(secretRef.getName());
    }

    return builder.build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public NFSVolumeSource mapNfsVolumeSource(io.fabric8.kubernetes.api.model.Volume volume) {
    return NFSVolumeSource.newBuilder()
        .setServer(volume.getNfs().getServer())
        .setPath(volume.getNfs().getPath())
        .setReadOnly(volume.getNfs().getReadOnly())
        .build();
  }

  public SecretVolumeSource mapSecretVolumeSource(
      io.fabric8.kubernetes.api.model.SecretVolumeSource secretVolumeSource) {
    SecretVolumeSource.Builder builder = SecretVolumeSource.newBuilder();

    if (Objects.nonNull(secretVolumeSource.getSecretName())) {
      builder.setSecretName(secretVolumeSource.getSecretName());
    }

    if (Objects.nonNull(secretVolumeSource.getItems())) {
      builder.addAllItems(mapKeyToPaths(secretVolumeSource.getItems()));
    }

    if (Objects.nonNull(secretVolumeSource.getDefaultMode())) {
      builder.setDefaultMode(secretVolumeSource.getDefaultMode());
    }

    if (Objects.nonNull(secretVolumeSource.getOptional())) {
      builder.setOptional(secretVolumeSource.getOptional());
    }

    return builder.build();
  }

  public Iterable<? extends KeyToPath> mapKeyToPaths(
      List<io.fabric8.kubernetes.api.model.KeyToPath> items) {
    return items.stream()
        .map(i -> {
          KeyToPath.Builder builder = KeyToPath.newBuilder();

          if (Objects.nonNull(i.getKey())) {
              builder.setKey(i.getKey());
          }

          if (Objects.nonNull(i.getPath())) {
              builder.setPath(i.getPath());
          }

          if (Objects.nonNull(i.getMode())) {
              builder.setMode(i.getMode());
          }

          return builder.build();
        })
        .collect(Collectors.toList());
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public GitRepoVolumeSource mapGitRepoVolumeSource(
      io.fabric8.kubernetes.api.model.Volume volume) {
    return GitRepoVolumeSource.newBuilder()
        .setRepository(volume.getGitRepo().getRepository())
        .setRevision(volume.getGitRepo().getRevision())
        .setDirectory(volume.getGitRepo().getDirectory())
        .build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public AWSElasticBlockStoreVolumeSource mapAwsElasticBlockStoreVolumeSource(
      io.fabric8.kubernetes.api.model.Volume volume) {
    return AWSElasticBlockStoreVolumeSource.newBuilder()
        .setVolumeID(volume.getAwsElasticBlockStore().getVolumeID())
        .setFsType(volume.getAwsElasticBlockStore().getFsType())
        .setPartition(volume.getAwsElasticBlockStore().getPartition())
        .setReadOnly(volume.getAwsElasticBlockStore().getReadOnly())
        .build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public GCEPersistentDiskVolumeSource mapGcePersistentDiskVolumeSource(
      io.fabric8.kubernetes.api.model.Volume volume) {
    return GCEPersistentDiskVolumeSource.newBuilder()
        .setPdName(volume.getGcePersistentDisk().getPdName())
        .setFsType(volume.getGcePersistentDisk().getFsType())
        .setPartition(volume.getGcePersistentDisk().getPartition())
        .setReadOnly(volume.getGcePersistentDisk().getReadOnly())
        .build();
  }

  public EmptyDirVolumeSource mapEmptyDirVolumeSource(
      io.fabric8.kubernetes.api.model.EmptyDirVolumeSource emptyDirVolumeSource) {
    EmptyDirVolumeSource.Builder builder = EmptyDirVolumeSource.newBuilder();

    if (Objects.nonNull(emptyDirVolumeSource.getMedium())) {
      builder.setMedium(emptyDirVolumeSource.getMedium());
    }

    if (Objects.nonNull(emptyDirVolumeSource.getSizeLimit())) {
      builder.setSizeLimit(mapQuantity(emptyDirVolumeSource.getSizeLimit()));
    }

    return builder.build();
  }

  @SuppressFBWarnings("UPM_UNCALLED_public_METHOD")
  public HostPathVolumeSource mapHostPathVolumeSource(
      io.fabric8.kubernetes.api.model.Volume volume) {
    if (volume.getHostPath() == null) {
      return HostPathVolumeSource.newBuilder().build();
    }

    return HostPathVolumeSource.newBuilder()
        .setPath(volume.getHostPath().getPath())
        .setType(volume.getHostPath().getType())
        .build();
  }

}
