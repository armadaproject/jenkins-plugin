package io.armadaproject.jenkins.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Event manager for subscribing to and publishing Armada job events. Uses a pub/sub pattern where
 * subscribers can register for events on specific job set IDs.
 *
 * <p>The most recent event per key (typically the job ID) is retained and replayed to subscribers
 * that register after it was published. A job set is watched by a single long-lived stream, so a
 * subscriber joining later - a second {@code armadaContainer} step on the same agent, say - would
 * otherwise never see an event the watcher already consumed.
 *
 * @param <T> the type of event to manage (e.g., JobRunningEvent)
 */
public class ArmadaEventManager<T> {

  private final ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<T>>> subscribers =
      new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, ConcurrentHashMap<String, T>> lastEvents =
      new ConcurrentHashMap<>();

  private final Function<T, String> keyExtractor;

  /**
   * @param keyExtractor derives the retention key from an event, so that one job set holds at most
   *                     one retained event per job
   */
  public ArmadaEventManager(Function<T, String> keyExtractor) {
    this.keyExtractor = keyExtractor;
  }

  /**
   * Subscribe to events for a specific job set ID. Any retained events for the job set are
   * delivered to the subscriber immediately, on the calling thread.
   *
   * @param jobSetId   the job set ID to subscribe to
   * @param subscriber the consumer that will be called when an event is published
   */
  public void subscribe(String jobSetId, Consumer<T> subscriber) {
    subscribers.computeIfAbsent(jobSetId, k -> new CopyOnWriteArrayList<>()).add(subscriber);

    // A publish racing with this replay can deliver the same event twice; consumers must be
    // idempotent.
    retained(jobSetId).values().forEach(subscriber);
  }

  /**
   * Unsubscribe from events for a specific job set ID.
   *
   * @param jobSetId   the job set ID to unsubscribe from
   * @param subscriber the consumer to remove
   */
  public void unsubscribe(String jobSetId, Consumer<T> subscriber) {
    CopyOnWriteArrayList<Consumer<T>> consumerList = subscribers.get(jobSetId);
    if (consumerList != null) {
      consumerList.remove(subscriber);
      if (consumerList.isEmpty()) {
        subscribers.remove(jobSetId);
      }
    }
  }

  /**
   * Publish an event to all subscribers for a specific job set ID, and retain it for subscribers
   * that register later.
   *
   * @param jobSetId the job set ID to publish to
   * @param event    the event to publish
   */
  public void publish(String jobSetId, T event) {
    lastEvents.computeIfAbsent(jobSetId, k -> new ConcurrentHashMap<>())
        .put(keyExtractor.apply(event), event);

    CopyOnWriteArrayList<Consumer<T>> consumerList = subscribers.get(jobSetId);
    if (consumerList != null) {
      for (Consumer<T> subscriber : consumerList) {
        subscriber.accept(event);
      }
    }
  }

  /**
   * Drops the retained event for a single key. A job set ID is shared by every job submitted in the
   * same period, so only the terminating job's entry is removed.
   *
   * @param jobSetId the job set ID the event was published to
   * @param key      the key the event was retained under (the job ID)
   */
  public void evict(String jobSetId, String key) {
    ConcurrentHashMap<String, T> events = lastEvents.get(jobSetId);
    if (events != null) {
      events.remove(key);
      if (events.isEmpty()) {
        lastEvents.remove(jobSetId);
      }
    }
  }

  private Map<String, T> retained(String jobSetId) {
    ConcurrentHashMap<String, T> events = lastEvents.get(jobSetId);
    return events != null ? events : Collections.emptyMap();
  }
}
