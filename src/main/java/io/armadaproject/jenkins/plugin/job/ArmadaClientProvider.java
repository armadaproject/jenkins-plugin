package io.armadaproject.jenkins.plugin.job;

import io.armadaproject.ArmadaClient;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;

public interface ArmadaClientProvider {
    ArmadaClient get() throws KubernetesAuthException;
}
