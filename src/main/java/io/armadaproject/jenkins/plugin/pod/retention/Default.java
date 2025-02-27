package io.armadaproject.jenkins.plugin.pod.retention;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.DescriptorVisibilityFilter;
import io.fabric8.kubernetes.api.model.Pod;
import java.io.Serializable;
import java.util.function.Supplier;
import io.armadaproject.jenkins.plugin.ArmadaCloud;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class Default extends PodRetention implements Serializable {

    private static final long serialVersionUID = -5209499689925746138L;

    @DataBoundConstructor
    public Default() {}

    @Override
    public boolean shouldDeletePod(ArmadaCloud cloud, Supplier<Pod> pod) {
        PodRetention parent = cloud.getPodRetention();
        if (!(parent instanceof Default)) {
            return parent.shouldDeletePod(cloud, pod);
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof Default) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return Messages._default();
    }

    @Extension
    public static class FilterImpl extends DescriptorVisibilityFilter {

        @Override
        @SuppressWarnings("rawtypes")
        public boolean filter(Object context, Descriptor descriptor) {
            if (context instanceof ArmadaCloud.DescriptorImpl && descriptor instanceof DescriptorImpl) {
                return false;
            }
            return true;
        }
    }

    @Extension
    @Symbol("default")
    public static class DescriptorImpl extends PodRetentionDescriptor {

        @Override
        public String getDisplayName() {
            return Messages._default();
        }
    }
}
