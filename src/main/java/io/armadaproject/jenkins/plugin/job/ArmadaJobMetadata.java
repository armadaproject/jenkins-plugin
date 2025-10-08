package io.armadaproject.jenkins.plugin.job;

import api.EventOuterClass;

public class ArmadaJobMetadata {
    private final String jobSetId;
    private final String jobId;
    private final String podName;
    private final String clusterId;
    private final String reason;
    private final EventOuterClass.Cause cause;

    public ArmadaJobMetadata(String jobSetId, String jobId, String podName, String clusterId) {
        this(jobSetId, jobId, podName, clusterId, null, null);
    }

    public ArmadaJobMetadata(String jobSetId, String jobId, String podName, String clusterId, String reason, EventOuterClass.Cause cause) {
        this.jobSetId = jobSetId;
        this.jobId = jobId;
        this.podName = podName;
        this.clusterId = clusterId;
        this.reason = reason;
        this.cause = cause;
    }

    public String getJobId() {
        return jobId;
    }

    public String getPodName() {
        return podName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public ArmadaJobMetadata mergeWith(final ArmadaJobMetadata other) {
        String podName;
        String clusterId;
        if (other.podName != null) {
            podName = other.podName;
        } else {
            podName = this.podName;
        }

        if (other.clusterId != null) {
            clusterId = other.clusterId;
        } else {
            clusterId = this.clusterId;
        }

        return new ArmadaJobMetadata(jobSetId, jobId, podName, clusterId);
    }

    public String getJobSetId() {
        return jobSetId;
    }

    public String getReason() {
        return reason;
    }

    public EventOuterClass.Cause getCause() {
        return cause;
    }
}
