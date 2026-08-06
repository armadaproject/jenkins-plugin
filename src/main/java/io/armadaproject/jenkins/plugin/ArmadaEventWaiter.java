package io.armadaproject.jenkins.plugin;

import static org.awaitility.Awaitility.await;

import api.EventOuterClass.JobRunningEvent;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Handles waiting for Armada job events. Extracts event subscription and waiting logic for
 * reusability and testability.
 */
public class ArmadaEventWaiter {

  private static final Logger LOGGER = Logger.getLogger(ArmadaEventWaiter.class.getName());

  private final ArmadaCloud cloud;

  public ArmadaEventWaiter(ArmadaCloud cloud) {
    this.cloud = cloud;
  }

  /**
   * Waits for a JobRunningEvent for the specified job.
   *
   * @param jobId    the job ID to wait for
   * @param jobSetId the job set ID containing the job
   * @return the JobRunningEvent when the job starts running
   * @throws IOException if timeout occurs or event is not received
   */
  public JobRunningEvent waitForJobRunning(String jobId, String jobSetId) throws IOException {
    ArmadaEventManager<JobRunningEvent> eventManager = cloud.getArmadaEventManager();
    AtomicReference<JobRunningEvent> matchedEvent = new AtomicReference<>();

    Consumer<JobRunningEvent> consumer = event -> {
      if (event.getJobId().equals(jobId)) {
        LOGGER.fine("Received JobRunningEvent for job: " + jobId);
        matchedEvent.set(event);
      }
    };

    eventManager.subscribe(jobSetId, consumer);

    // Start watching events if not already started. computeIfAbsent, not putIfAbsent: the latter
    // evaluates its argument eagerly and would start a watcher thread on every call.
    cloud.getJobSetIdThreads().computeIfAbsent(jobSetId, cloud::startWatchingArmadaEvents);

    try {
      LOGGER.fine("Waiting for JobRunningEvent for job: " + jobId + " in job set: " + jobSetId);

      await()
          .atMost(ArmadaPluginConfig.EVENT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .pollInterval(ArmadaPluginConfig.POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
          .until(() -> matchedEvent.get() != null);

      JobRunningEvent event = matchedEvent.get();
      if (event == null) {
        throw new IOException(String.format("Timeout waiting for job %s to reach running state after %d %s",
            jobId, ArmadaPluginConfig.EVENT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS.toString().toLowerCase()));
      }

      LOGGER.fine("Successfully received JobRunningEvent for job: " + jobId);
      return event;

    } catch (IOException e) {
      throw e; // Re-throw IOException as-is
    } catch (Exception e) {
      throw new IOException(String.format("Timeout waiting for job %s to reach running state after %d %s",
          jobId, ArmadaPluginConfig.EVENT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS.toString().toLowerCase()), e);
    } finally {
      eventManager.unsubscribe(jobSetId, consumer);
    }
  }
}
