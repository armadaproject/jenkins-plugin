package io.armadaproject.jenkins.plugin;

import static org.awaitility.Awaitility.await;

import api.EventOuterClass.JobPendingEvent;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.awaitility.core.ConditionTimeoutException;

/**
 * Buffers JobPendingEvents for a job set so the caller can subscribe before the job is submitted
 * and then await a specific job id. Subscribing after submission would race the watcher: the
 * event manager has no replay, so a pending event arriving before the subscribe call is lost.
 *
 * <p>Caller is responsible for closing this to unsubscribe.
 */
final class PendingEventSubscription implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(PendingEventSubscription.class.getName());

  private final ArmadaEventManager<JobPendingEvent> eventManager;
  private final String jobSetId;
  private final ConcurrentHashMap<String, JobPendingEvent> received = new ConcurrentHashMap<>();
  private final Consumer<JobPendingEvent> consumer = e -> received.putIfAbsent(e.getJobId(), e);

  static PendingEventSubscription start(ArmadaCloud cloud, String jobSetId) {
    return new PendingEventSubscription(cloud, jobSetId);
  }

  private PendingEventSubscription(ArmadaCloud cloud, String jobSetId) {
    this.eventManager = cloud.getArmadaPendingEventManager();
    this.jobSetId = jobSetId;
    eventManager.subscribe(jobSetId, consumer);
    cloud.getJobSetIdThreads().putIfAbsent(jobSetId,
        cloud.startWatchingArmadaEvents(jobSetId));
  }

  JobPendingEvent awaitFor(String jobId) throws IOException {
    LOGGER.fine(() -> "Awaiting JobPendingEvent for job: " + jobId + " in job set: " + jobSetId);
    try {
      await()
          .atMost(ArmadaPluginConfig.EVENT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .pollInterval(ArmadaPluginConfig.POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
          .until(() -> received.containsKey(jobId));
    } catch (ConditionTimeoutException e) {
      throw new IOException(String.format(
          "Timeout waiting for job %s to reach pending state after %d seconds",
          jobId, ArmadaPluginConfig.EVENT_WAIT_TIMEOUT_SECONDS), e);
    }
    JobPendingEvent event = received.get(jobId);
    if (event == null) {
      throw new IOException("Pending event vanished from buffer for job: " + jobId);
    }
    return event;
  }

  @Override
  public void close() {
    eventManager.unsubscribe(jobSetId, consumer);
    received.clear();
  }
}
