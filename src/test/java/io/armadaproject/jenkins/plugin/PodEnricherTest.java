package io.armadaproject.jenkins.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import org.junit.jupiter.api.Test;

class PodEnricherTest {

  @Test
  void jnlpSecretEnvVarIsResolvedFromSecretKeyRefAndNotPlainText() {
    String agentName = "c-5e05d0ba-f020-48a9-bfb4-99efb3b0e425";

    EnvVar env = PodEnricher.buildJnlpSecretEnvVar(agentName);

    assertEquals(ArmadaPluginConfig.JENKINS_SECRET_ENV, env.getName());
    // The JNLP MAC must NOT appear as a literal value in the PodSpec.
    assertNull(env.getValue(), "JENKINS_SECRET must not be a plaintext value in the PodSpec");

    assertNotNull(env.getValueFrom());
    SecretKeySelector selector = env.getValueFrom().getSecretKeyRef();
    assertNotNull(selector, "JENKINS_SECRET must be sourced from a secretKeyRef");
    assertEquals(agentName + ArmadaPluginConfig.JNLP_SECRET_NAME_SUFFIX, selector.getName());
    assertEquals(ArmadaPluginConfig.JNLP_SECRET_KEY, selector.getKey());
    assertFalse(Boolean.TRUE.equals(selector.getOptional()),
        "JNLP secret must be required, not optional");
    assertTrue(selector.getName().endsWith(ArmadaPluginConfig.JNLP_SECRET_NAME_SUFFIX));
  }
}
