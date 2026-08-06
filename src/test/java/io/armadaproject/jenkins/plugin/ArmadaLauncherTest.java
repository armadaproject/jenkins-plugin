package io.armadaproject.jenkins.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import org.junit.jupiter.api.Test;

class ArmadaLauncherTest {

  @Test
  void buildJnlpSecretShapeMatchesSecretKeyRefInPodEnricher() {
    String agentName = "c-5e05d0ba-f020-48a9-bfb4-99efb3b0e425";
    String secretName = agentName + ArmadaPluginConfig.JNLP_SECRET_NAME_SUFFIX;
    String mac = "abcdef0123456789";
    Pod pod = new PodBuilder()
        .withNewMetadata()
          .withName("agent-pod")
          .withNamespace("armada")
          .withUid("pod-uid-1")
        .endMetadata()
        .build();

    Secret secret = ArmadaLauncher.buildJnlpSecret(pod, mac, secretName);

    assertEquals(secretName, secret.getMetadata().getName());
    assertEquals("armada", secret.getMetadata().getNamespace());
    assertEquals("Opaque", secret.getType());
    // The data key must match what PodEnricher's secretKeyRef requests, otherwise the kubelet
    // would block the jnlp container with CreateContainerConfigError.
    assertEquals(mac, secret.getStringData().get(ArmadaPluginConfig.JNLP_SECRET_KEY));

    assertEquals(1, secret.getMetadata().getOwnerReferences().size());
    OwnerReference owner = secret.getMetadata().getOwnerReferences().get(0);
    assertEquals("Pod", owner.getKind());
    assertEquals("v1", owner.getApiVersion());
    assertEquals("agent-pod", owner.getName());
    assertEquals("pod-uid-1", owner.getUid());
    // Controller + blockOwnerDeletion together make kube GC remove this Secret when the Pod dies.
    assertTrue(Boolean.TRUE.equals(owner.getController()));
    assertTrue(Boolean.TRUE.equals(owner.getBlockOwnerDeletion()));
  }

  @Test
  void buildJnlpSecretNameAndKeyAlignWithPodEnricherEnvVar() {
    String agentName = "c-some-agent";
    String secretName = agentName + ArmadaPluginConfig.JNLP_SECRET_NAME_SUFFIX;
    Pod pod = new PodBuilder()
        .withNewMetadata().withName("p").withNamespace("ns").withUid("u").endMetadata()
        .build();

    Secret secret = ArmadaLauncher.buildJnlpSecret(pod, "mac", secretName);

    // Cross-check: the env var built by PodEnricher must point at this exact Secret + key.
    var envSelector = PodEnricher.buildJnlpSecretEnvVar(agentName)
        .getValueFrom().getSecretKeyRef();
    assertEquals(secret.getMetadata().getName(), envSelector.getName());
    assertNotNull(secret.getStringData().get(envSelector.getKey()),
        "Secret must contain the data key referenced by PodEnricher");
  }
}
