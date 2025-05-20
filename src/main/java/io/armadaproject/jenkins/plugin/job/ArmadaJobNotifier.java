package io.armadaproject.jenkins.plugin.job;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ArmadaJobNotifier {
    public interface Callback {
        void accept(ArmadaJobMetadata metadata);

        void error(RuntimeException e);

        void cancelled();
    }

    private final ConcurrentMap<String, List<Callback>> callbacks = new ConcurrentHashMap<>();

    public void close() {
        callbacks.forEach((jobId, callbacks) -> callbacks.forEach(Callback::cancelled));
        callbacks.clear();
    }

    public void subscribe(String jobId, Callback callback) {
        callbacks.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>());
        callbacks.get(jobId).add(callback);
    }

    public void unsubscribe(String jobId, Callback callback) {
        var subscriptions = callbacks.get(jobId);
        if(subscriptions != null) {
            subscriptions.remove(callback);
        }
    }

    public void notify(ArmadaJobMetadata metadata, RuntimeException error) {
        var list = callbacks.get(metadata.getJobId());
        if (list != null) {
            list.forEach(callback -> {
                if (error != null) {
                    callback.error(error);
                } else {
                    callback.accept(metadata);
                }
            });
        }
    }
}
