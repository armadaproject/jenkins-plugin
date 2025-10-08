package io.armadaproject.jenkins.plugin.job;

import api.EventOuterClass;
import api.SubmitOuterClass;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArmadaClientUtil {
    private ArmadaClientUtil() {}

    public static final Set<SubmitOuterClass.JobState> TERMINAL_STATES = new HashSet<>(List.of(SubmitOuterClass.JobState.FAILED, SubmitOuterClass.JobState.CANCELLED, SubmitOuterClass.JobState.SUCCEEDED, SubmitOuterClass.JobState.PREEMPTED));

    public static String lookoutUrlForJob(String lookoutBaseUrl, int lookoutPort, String queue, String jobSetId, String jobId) {
        return StringUtils.stripEnd(lookoutBaseUrl, "/") + ":" + lookoutPort + "/?page=0&f[0][id]=queue&f[0][value][0]=" +
                queue + "&f[0][match]=anyOf&f[1][id]=jobSet&f[1][value]=" +
                jobSetId + "&f[1][match]=exact&f[2][id]=jobId&f[2][value]=" +
                jobId + "&f[2][match]=exact";
    }
    
    public static SubmitOuterClass.JobState toJobState(EventOuterClass.EventMessage.EventsCase event) {
        switch(event) {
            case RUNNING:
                return SubmitOuterClass.JobState.RUNNING;
            case PENDING:
                return SubmitOuterClass.JobState.PENDING;
            case QUEUED:
                return SubmitOuterClass.JobState.QUEUED;
            case SUCCEEDED:
                return SubmitOuterClass.JobState.SUCCEEDED;
            case FAILED:
                return SubmitOuterClass.JobState.FAILED;
            case SUBMITTED:
                return SubmitOuterClass.JobState.SUBMITTED;
            case LEASED:
                return SubmitOuterClass.JobState.LEASED;
            case PREEMPTED:
                return SubmitOuterClass.JobState.PREEMPTED;
            case CANCELLED:
                return SubmitOuterClass.JobState.CANCELLED;
            default:
                return SubmitOuterClass.JobState.UNKNOWN;
        }
    }
    
    public static boolean isInFailedState(SubmitOuterClass.JobState jobState) {
        switch(jobState) {
            case FAILED:
            case REJECTED:
                return true;
            default:
                return false;
        }
    }

    public static boolean isInFailedState(EventOuterClass.EventMessage.EventsCase jobState) {
        switch(jobState) {
            case FAILED:
            case UNABLE_TO_SCHEDULE:
                return true;
            default:
                return false;
        }
    }

    public static boolean isInTerminalState(SubmitOuterClass.JobState jobState) {
        switch(jobState) {
            case FAILED:
            case CANCELLED:
            case SUCCEEDED:
            case PREEMPTED:
                return true;
            default:
                return false;
        }
    }

    public static boolean isInTerminalState(EventOuterClass.EventMessage.EventsCase eventsCase) {
        switch(eventsCase) {
            case FAILED:
            case CANCELLED:
            case SUCCEEDED:
            case PREEMPTED:
                return true;
            default:
                return false;
        }
    }

    public static ArmadaJobMetadata extractMetadata(EventOuterClass.EventMessage eventMessage) {
        String jobId = null;
        String jobSetId = null;
        String clusterId = null;
        String podName = null;
        String reason = null;
        EventOuterClass.Cause cause = null;
        switch(eventMessage.getEventsCase()) {
            case SUBMITTED:
                jobId = eventMessage.getSubmitted().getJobId();
                jobSetId = eventMessage.getSubmitted().getJobSetId();
                break;
            case QUEUED:
                jobId = eventMessage.getQueued().getJobId();
                jobSetId = eventMessage.getQueued().getJobSetId();
                break;
            case LEASED:
                jobId = eventMessage.getLeased().getJobId();
                jobSetId = eventMessage.getLeased().getJobSetId();
                break;
            case LEASE_RETURNED:
                jobId = eventMessage.getLeaseReturned().getJobId();
                jobSetId = eventMessage.getLeaseReturned().getJobSetId();
                break;
            case LEASE_EXPIRED:
                jobId = eventMessage.getLeaseExpired().getJobId();
                jobSetId = eventMessage.getLeaseExpired().getJobSetId();
                break;
            case PENDING:
                jobId = eventMessage.getPending().getJobId();
                jobSetId = eventMessage.getPending().getJobSetId();
                break;
            case RUNNING:
                var running = eventMessage.getRunning();
                jobSetId = running.getJobSetId();
                jobId = running.getJobId();
                clusterId = running.getClusterId();
                podName = running.getPodName();
                break;
            case UNABLE_TO_SCHEDULE:
                jobId = eventMessage.getUnableToSchedule().getJobId();
                jobSetId = eventMessage.getUnableToSchedule().getJobSetId();
                reason = eventMessage.getUnableToSchedule().getReason();
                break;
            case FAILED:
                jobId = eventMessage.getFailed().getJobId();
                jobSetId = eventMessage.getFailed().getJobSetId();
                reason = eventMessage.getFailed().getReason();
                cause = eventMessage.getFailed().getCause();
                break;
            case SUCCEEDED:
                jobId = eventMessage.getSucceeded().getJobId();
                jobSetId = eventMessage.getSucceeded().getJobSetId();
                break;
            case REPRIORITIZED:
                jobId = eventMessage.getReprioritized().getJobId();
                jobSetId = eventMessage.getReprioritized().getJobSetId();
                break;
            case CANCELLING:
                jobId = eventMessage.getCancelling().getJobId();
                jobSetId = eventMessage.getCancelling().getJobSetId();
                reason = eventMessage.getCancelling().getReason();
                break;
            case CANCELLED:
                jobId = eventMessage.getCancelled().getJobId();
                jobSetId = eventMessage.getCancelled().getJobSetId();
                reason = eventMessage.getCancelled().getReason();
                break;
            case UTILISATION:
                jobId = eventMessage.getUtilisation().getJobId();
                jobSetId = eventMessage.getUtilisation().getJobSetId();
                break;
            case INGRESS_INFO:
                jobId = eventMessage.getIngressInfo().getJobId();
                jobSetId = eventMessage.getIngressInfo().getJobSetId();
                break;
            case REPRIORITIZING:
                jobId = eventMessage.getReprioritizing().getJobId();
                jobSetId = eventMessage.getReprioritizing().getJobSetId();
                break;
            case PREEMPTED:
                jobId = eventMessage.getPreempted().getJobId();
                jobSetId = eventMessage.getPreempted().getJobSetId();
                reason = eventMessage.getPreempted().getReason();
                break;
            case PREEMPTING:
                jobId = eventMessage.getPreempting().getJobId();
                jobSetId = eventMessage.getPreempting().getJobSetId();
                reason = eventMessage.getPreempted().getReason();
        }

        return new ArmadaJobMetadata(jobSetId, jobId, podName, clusterId, reason, cause);
    }
}
