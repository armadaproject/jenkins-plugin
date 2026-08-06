package io.armadaproject.jenkins.plugin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ArmadaEventManagerTest {

  private static final String JOB_SET_ID = "job-set-1";

  /** Events are their own key, so "job-1:running" is retained under "job-1". */
  private static ArmadaEventManager<String> newManager() {
    return new ArmadaEventManager<>(event -> event.split(":")[0]);
  }

  @Test
  void subscriberRegisteredBeforePublishReceivesEvent() {
    ArmadaEventManager<String> manager = newManager();
    List<String> received = new ArrayList<>();

    manager.subscribe(JOB_SET_ID, received::add);
    manager.publish(JOB_SET_ID, "job-1:running");

    assertEquals(List.of("job-1:running"), received);
  }

  @Test
  void subscriberRegisteredAfterPublishReceivesRetainedEvent() {
    // The regression: a second armadaContainer step subscribes long after the watcher consumed the
    // JobRunningEvent, and used to wait for an event that would never be republished.
    ArmadaEventManager<String> manager = newManager();
    List<String> received = new ArrayList<>();

    manager.publish(JOB_SET_ID, "job-1:running");
    manager.subscribe(JOB_SET_ID, received::add);

    assertEquals(List.of("job-1:running"), received);
  }

  @Test
  void latestEventPerKeyIsRetained() {
    ArmadaEventManager<String> manager = newManager();
    List<String> received = new ArrayList<>();

    manager.publish(JOB_SET_ID, "job-1:pending");
    manager.publish(JOB_SET_ID, "job-1:running");
    manager.subscribe(JOB_SET_ID, received::add);

    assertEquals(List.of("job-1:running"), received);
  }

  @Test
  void eventsForDifferentKeysAreAllReplayed() {
    ArmadaEventManager<String> manager = newManager();
    List<String> received = new ArrayList<>();

    manager.publish(JOB_SET_ID, "job-1:running");
    manager.publish(JOB_SET_ID, "job-2:running");
    manager.subscribe(JOB_SET_ID, received::add);

    assertEquals(2, received.size());
    assertTrue(received.containsAll(List.of("job-1:running", "job-2:running")));
  }

  @Test
  void evictDropsOnlyTheEvictedKey() {
    ArmadaEventManager<String> manager = newManager();
    List<String> received = new ArrayList<>();

    manager.publish(JOB_SET_ID, "job-1:running");
    manager.publish(JOB_SET_ID, "job-2:running");
    manager.evict(JOB_SET_ID, "job-1");
    manager.subscribe(JOB_SET_ID, received::add);

    assertEquals(List.of("job-2:running"), received);
  }

  @Test
  void unsubscribedConsumerStopsReceivingEvents() {
    ArmadaEventManager<String> manager = newManager();
    List<String> received = new ArrayList<>();
    Consumer<String> consumer = received::add;

    manager.subscribe(JOB_SET_ID, consumer);
    manager.unsubscribe(JOB_SET_ID, consumer);
    manager.publish(JOB_SET_ID, "job-1:running");

    assertTrue(received.isEmpty());
  }

  @Test
  void publishAndEvictWithoutSubscribersDoNotThrow() {
    ArmadaEventManager<String> manager = newManager();

    assertDoesNotThrow(() -> manager.publish(JOB_SET_ID, "job-1:running"));
    assertDoesNotThrow(() -> manager.evict(JOB_SET_ID, "job-1"));
    assertDoesNotThrow(() -> manager.evict("unknown-job-set", "job-1"));
  }
}
