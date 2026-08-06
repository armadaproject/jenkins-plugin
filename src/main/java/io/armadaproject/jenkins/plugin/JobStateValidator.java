package io.armadaproject.jenkins.plugin;

import api.SubmitOuterClass.JobState;
import java.io.IOException;

/**
 * Validates Armada job states and provides state machine logic.
 */
public class JobStateValidator {

  private JobStateValidator() {
    // Utility class - prevent instantiation
  }

  /**
   * Validates that a job state is acceptable for continuing execution. Throws IOException only for
   * terminal states, from which the job can never reach or return to RUNNING.
   *
   * <p>{@code UNKNOWN} and {@code null} are <em>not</em> failures: Armada reports them for a job it
   * has not ingested yet, and for a job id missing from the status response map. Both are transient
   * and callers must keep polling.
   *
   * @param state the job state to validate, may be null
   * @param jobId the job ID for error messages
   * @throws IOException if the state is terminal
   */
  public static void validate(JobState state, String jobId) throws IOException {
    if (state == JobState.FAILED || state == JobState.REJECTED) {
      throw new IOException(String.format("Job %s in state %s: Job entered failed state", jobId, state.name()));
    }
    if (state == JobState.CANCELLED) {
      throw new IOException(String.format("Job %s in state CANCELLED: Job was cancelled", jobId));
    }
    if (state == JobState.PREEMPTED) {
      throw new IOException(String.format("Job %s in state PREEMPTED: Job was preempted", jobId));
    }
    if (state == JobState.SUCCEEDED) {
      throw new IOException(String.format("Job %s in state SUCCEEDED: Job terminated unexpectedly", jobId));
    }
  }

  /**
   * Checks if a job state is terminal (no further state changes expected).
   *
   * @param state the job state to check, may be null
   * @return true if the state is terminal
   */
  public static boolean isTerminal(JobState state) {
    return state == JobState.SUCCEEDED
        || state == JobState.FAILED
        || state == JobState.REJECTED
        || state == JobState.PREEMPTED
        || state == JobState.CANCELLED;
  }

  /**
   * Checks if a job state indicates the job is running.
   *
   * @param state the job state to check, may be null
   * @return true if the job is running
   */
  public static boolean isRunning(JobState state) {
    return state == JobState.RUNNING;
  }

  /**
   * Checks if a job state is a non-terminal state the job may still leave on its way to RUNNING.
   * {@code UNKNOWN} and {@code null} count as pending, since Armada reports those while a job is
   * still being ingested.
   *
   * @param state the job state to check, may be null
   * @return true if the job may still reach RUNNING
   */
  public static boolean isPending(JobState state) {
    return !isTerminal(state) && !isRunning(state);
  }

  /**
   * Checks if a job state indicates failure.
   *
   * @param state the job state to check, may be null
   * @return true if the job failed
   */
  public static boolean isFailed(JobState state) {
    return state == JobState.FAILED
        || state == JobState.REJECTED
        || state == JobState.PREEMPTED;
  }
}
