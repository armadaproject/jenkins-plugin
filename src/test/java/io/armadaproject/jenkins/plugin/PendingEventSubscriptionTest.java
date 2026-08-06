package io.armadaproject.jenkins.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.EventOuterClass.JobPendingEvent;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingEventSubscriptionTest {

  private static final String JOB_SET = "set-1";

  private ArmadaCloud cloud;
  private ArmadaEventManager<JobPendingEvent> manager;

  @BeforeEach
  void setUp() {
    cloud = mock(ArmadaCloud.class);
    manager = new ArmadaEventManager<>();
    when(cloud.getArmadaPendingEventManager()).thenReturn(manager);
    lenient().when(cloud.getJobSetIdThreads()).thenReturn(new ConcurrentHashMap<>());
    // The subscription starts the watcher; stub it out so no real Armada client is opened.
    lenient().when(cloud.startWatchingArmadaEvents(JOB_SET))
        .thenReturn(new Thread(() -> {}, "test-noop"));
  }

  @Test
  void awaitForReturnsMatchingEventPublishedAfterSubscribe() throws IOException {
    try (PendingEventSubscription sub = PendingEventSubscription.start(cloud, JOB_SET)) {
      manager.publish(JOB_SET, pendingEvent("job-a", "c1", "p", "ns"));

      JobPendingEvent got = sub.awaitFor("job-a");

      assertEquals("c1", got.getClusterId());
      assertEquals("p", got.getPodName());
      assertEquals("ns", got.getPodNamespace());
    }
  }

  @Test
  void awaitForFiltersByJobIdAcrossNoisyJobSet() throws IOException {
    try (PendingEventSubscription sub = PendingEventSubscription.start(cloud, JOB_SET)) {
      manager.publish(JOB_SET, pendingEvent("other-1", "x", "x", "x"));
      manager.publish(JOB_SET, pendingEvent("other-2", "x", "x", "x"));
      manager.publish(JOB_SET, pendingEvent("mine", "cMine", "p", "ns"));

      JobPendingEvent got = sub.awaitFor("mine");

      assertEquals("cMine", got.getClusterId());
    }
  }

  @Test
  void awaitForReturnsEventPublishedBeforeAwaitCall() throws IOException {
    // Subscribe-before-submit invariant: an event arriving between subscribe and awaitFor
    // must be in the buffer when awaitFor runs.
    try (PendingEventSubscription sub = PendingEventSubscription.start(cloud, JOB_SET)) {
      manager.publish(JOB_SET, pendingEvent("early", "cEarly", "p", "ns"));

      assertEquals("cEarly", sub.awaitFor("early").getClusterId());
    }
  }

  private static JobPendingEvent pendingEvent(String jobId, String clusterId, String podName,
      String namespace) {
    return JobPendingEvent.newBuilder()
        .setJobId(jobId)
        .setClusterId(clusterId)
        .setPodName(podName)
        .setPodNamespace(namespace)
        .build();
  }
}
