package io.armadaproject.jenkins.plugin.pod.retention;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.function.Supplier;
import io.armadaproject.jenkins.plugin.ArmadaCloud;

/**
 * <code>PodRetention</code> instances determine if the Kubernetes pod running a Jenkins agent
 * should be deleted after Jenkins terminates the agent.
 *
 * <p>Custom pod retention behavior can be added by extending this class, including a descriptor
 * that extends {@link PodRetentionDescriptor}</p>
 */
public abstract class PodRetention extends AbstractDescribableImpl<PodRetention> implements ExtensionPoint {

    /**
     * Returns the default <code>PodRetention</code> for a <code>KubernetesCloud</code> instance.
     *
     * @return the {@link Never} <code>PodRetention</code> strategy.
     */
    public static PodRetention getKubernetesCloudDefault() {
        return new Never();
    }

    /**
     * Returns the default <code>PodRetention</code> for a <code>PodTemplate</code> instance.
     *
     * @return the {@link Default} <code>PodRetention</code> strategy.
     */
    public static PodRetention getPodTemplateDefault() {
        return new Default();
    }

    /**
     * Determines if a agent pod should be deleted after the Jenkins build completes.
     *
     * @param cloud - the {@link ArmadaCloud} the agent pod belongs to.
     * @param pod - the {@link Pod} running the Jenkins build.
     *
     * @return <code>true</code> if the agent pod should be deleted.
     */
    public abstract boolean shouldDeletePod(ArmadaCloud cloud, Supplier<Pod> pod);

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
