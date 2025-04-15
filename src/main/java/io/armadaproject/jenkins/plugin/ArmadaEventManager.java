package io.armadaproject.jenkins.plugin;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ArmadaEventManager<T> {
  private final CopyOnWriteArrayList<Consumer<T>> subscribers = new CopyOnWriteArrayList<>();

  public void subscribe(Consumer<T> subscriber) {
    subscribers.add(subscriber);
  }

  public void unsubscribe(Consumer<T> subscriber) {
    subscribers.remove(subscriber);
  }

  public void publish(T event) {
    for (Consumer<T> subscriber : subscribers) {
      subscriber.accept(event);
    }
  }
}