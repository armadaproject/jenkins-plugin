package io.armadaproject.jenkins.plugin.job;

import hudson.slaves.OfflineCause;
import org.kohsuke.stapler.export.Exported;

public class ArmadaLaunchFailedOfflineCause extends OfflineCause {
    public final String description;

    public ArmadaLaunchFailedOfflineCause(String description) {
        this.description = description;
    }

    @Exported(name = "description")
    public String toString() {
        return this.description;
    }
}
