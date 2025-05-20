package io.armadaproject.jenkins.plugin.job;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ArmadaGarbageCollection {
    @Extension
    public static final class PeriodicGarbageCollection extends AsyncPeriodicWork {
        public PeriodicGarbageCollection() {
            super("Periodic cleanup of armada plugin state and jobs");
        }

        @Override
        protected void execute(TaskListener listener) throws IOException, InterruptedException {
            var state = ArmadaState.getInstance();
            state.runCleanup();
            state.save();
        }

        @Override
        public long getRecurrencePeriod() {
            return TimeUnit.MINUTES.toMillis(5);
        }
    }
}
