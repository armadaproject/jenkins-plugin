package io.armadaproject.jenkins.plugin.job;

import api.EventOuterClass;

public interface ArmadaEventWatcher {
    void onClose();

    void onEvent(EventOuterClass.EventMessage message);
}
