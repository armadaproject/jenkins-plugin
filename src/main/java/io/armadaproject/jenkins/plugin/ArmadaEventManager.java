package io.armadaproject.jenkins.plugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ArmadaEventManager<T> {

  private final ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<T>>> subscribers =
      new ConcurrentHashMap<>();

  public void subscribe(String jobSetId, Consumer<T> subscriber) {
    subscribers.computeIfAbsent(jobSetId, k -> new CopyOnWriteArrayList<>()).add(subscriber);
  }

  public void unsubscribe(String jobSetId, Consumer<T> subscriber) {
    CopyOnWriteArrayList<Consumer<T>> consumerList = subscribers.get(jobSetId);
    if (consumerList != null) {
      consumerList.remove(subscriber);
      // TODO check if this is necessary
      if (consumerList.isEmpty()) {
        subscribers.remove(jobSetId);
      }
    }
  }

  public void publish(String jobSetId, T event) {
    CopyOnWriteArrayList<Consumer<T>> consumerList = subscribers.get(jobSetId);
    if (consumerList != null) {
      for (Consumer<T> subscriber : consumerList) {
        subscriber.accept(event);
      }
    }
  }
}