package io.armadaproject.jenkins.plugin.job;

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
}
