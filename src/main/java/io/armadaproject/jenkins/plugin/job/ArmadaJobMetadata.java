package io.armadaproject.jenkins.plugin.job;

public class ArmadaJobMetadata {
    private final String jobSetId;
    private final String jobId;
    private final String podName;
    private final String clusterId;

    public ArmadaJobMetadata(String jobSetId, String jobId, String podName, String clusterId) {
        this.jobSetId = jobSetId;
        this.jobId = jobId;
        this.podName = podName;
        this.clusterId = clusterId;
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
}
