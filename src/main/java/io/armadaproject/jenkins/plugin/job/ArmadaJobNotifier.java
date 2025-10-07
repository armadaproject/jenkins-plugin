package io.armadaproject.jenkins.plugin.job;

import api.SubmitOuterClass;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ArmadaJobNotifier {
    public interface Callback {
        void accept(ArmadaJobMetadata metadata);

        void error(RuntimeException e);

        void cancelled();
    }

    private static class Key {
        private final String jobId;
        private final SubmitOuterClass.JobState state;

        private Key(String jobId, SubmitOuterClass.JobState state) {
            this.jobId = jobId;
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equals(jobId, key.jobId) && state == key.state;
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, state);
        }
    }

    private final ConcurrentMap<Key, List<Callback>> callbacks = new ConcurrentHashMap<>();

    public void close() {
        callbacks.forEach((jobId, callbacks) -> callbacks.forEach(Callback::cancelled));
        callbacks.clear();
    }

    public void subscribe(String jobId, SubmitOuterClass.JobState state, Callback callback) {
        var key = new Key(jobId, state);
        callbacks.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void unsubscribe(String jobId, SubmitOuterClass.JobState state, Callback callback) {
        var key = new Key(jobId, state);
        var subscriptions = callbacks.get(key);
        if(subscriptions != null) {
            subscriptions.remove(callback);
            if(subscriptions.isEmpty()) {
                callbacks.remove(key);
            }
        }
    }

    public void notify(ArmadaJobMetadata metadata, SubmitOuterClass.JobState state, RuntimeException error) {
        var key = new Key(metadata.getJobId(), state);
        var list = callbacks.get(key);
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
