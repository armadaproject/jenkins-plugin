package io.armadaproject.jenkins.plugin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.SubmitOuterClass.JobState;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class JobStateValidatorTest {

  private static final String JOB_ID = "job-1";

  @ParameterizedTest
  @EnumSource(value = JobState.class, names = {"QUEUED", "PENDING", "SUBMITTED", "LEASED",
      "RUNNING", "UNKNOWN"})
  void nonTerminalStatesDoNotThrow(JobState state) {
    assertDoesNotThrow(() -> JobStateValidator.validate(state, JOB_ID));
  }

  @Test
  void unknownJobIdDoesNotThrow() {
    // Armada omits a job from the status map until it has ingested it.
    assertDoesNotThrow(() -> JobStateValidator.validate(null, JOB_ID));
    assertTrue(JobStateValidator.isPending(null));
    assertFalse(JobStateValidator.isRunning(null));
    assertFalse(JobStateValidator.isTerminal(null));
  }

  @ParameterizedTest
  @EnumSource(value = JobState.class, names = {"FAILED", "REJECTED", "CANCELLED", "PREEMPTED",
      "SUCCEEDED"})
  void terminalStatesThrow(JobState state) {
    IOException e = assertThrows(IOException.class,
        () -> JobStateValidator.validate(state, JOB_ID));
    assertTrue(e.getMessage().contains(JOB_ID));
    assertTrue(e.getMessage().contains(state.name()));
    assertTrue(JobStateValidator.isTerminal(state));
    assertFalse(JobStateValidator.isPending(state));
  }

  @ParameterizedTest
  @EnumSource(value = JobState.class, names = {"RUNNING", "SUCCEEDED", "FAILED", "REJECTED",
      "CANCELLED", "PREEMPTED", "UNRECOGNIZED"}, mode = Mode.EXCLUDE)
  void statesBeforeRunningArePending(JobState state) {
    assertTrue(JobStateValidator.isPending(state));
  }

  @Test
  void runningIsNeitherPendingNorTerminal() {
    assertTrue(JobStateValidator.isRunning(JobState.RUNNING));
    assertFalse(JobStateValidator.isPending(JobState.RUNNING));
    assertFalse(JobStateValidator.isTerminal(JobState.RUNNING));
    assertFalse(JobStateValidator.isFailed(JobState.RUNNING));
  }

  @Test
  void failedStatesAreReportedAsFailures() {
    assertTrue(JobStateValidator.isFailed(JobState.FAILED));
    assertTrue(JobStateValidator.isFailed(JobState.REJECTED));
    assertTrue(JobStateValidator.isFailed(JobState.PREEMPTED));
    assertFalse(JobStateValidator.isFailed(JobState.SUCCEEDED));
    assertFalse(JobStateValidator.isFailed(JobState.CANCELLED));
    assertFalse(JobStateValidator.isFailed(JobState.UNKNOWN));
  }
}
