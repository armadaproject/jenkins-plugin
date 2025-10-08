package io.armadaproject.jenkins.plugin.job;

import java.util.Objects;

public class ArmadaClientParameters {
    public final String apiUrl;
    public final int apiPort;
    public final String queue;
    public final String namespace;
    public final String credentialsId;
    public final ArmadaJobSetStrategy jobSetStrategy;

    public ArmadaClientParameters(String apiUrl, int apiPort, String queue, String namespace, String credentialsId, ArmadaJobSetStrategy jobSetStrategy) {
        this.apiUrl = apiUrl;
        this.apiPort = apiPort;
        this.queue = queue;
        this.namespace = namespace;
        this.credentialsId = credentialsId;
        this.jobSetStrategy = jobSetStrategy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArmadaClientParameters)) return false;
        ArmadaClientParameters that = (ArmadaClientParameters) o;
        return apiPort == that.apiPort && Objects.equals(apiUrl, that.apiUrl) && Objects.equals(queue, that.queue) && Objects.equals(namespace, that.namespace) && Objects.equals(credentialsId, that.credentialsId) && Objects.equals(jobSetStrategy, that.jobSetStrategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiUrl, apiPort, queue, namespace, credentialsId, jobSetStrategy);
    }
}
