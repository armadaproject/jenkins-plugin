package io.armadaproject.jenkins.plugin;

import hudson.ExtensionList;
import hudson.ExtensionPoint;

/**
 * A factory of {@link ArmadaComputer} instances.
 */
public abstract class ArmadaComputerFactory implements ExtensionPoint {
    /**
     * Returns all registered implementations of {@link ArmadaComputerFactory}.
     * @return all registered implementations of {@link ArmadaComputerFactory}.
     */
    public static ExtensionList<ArmadaComputerFactory> all() {
        return ExtensionList.lookup(ArmadaComputerFactory.class);
    }

    /**
     * Returns a new instance of {@link ArmadaComputer}.
     * @return a new instance of {@link ArmadaComputer}.
     */
    public static ArmadaComputer createInstance(ArmadaSlave slave) {
        for (ArmadaComputerFactory factory : all()) {
            ArmadaComputer kubernetesComputer = factory.newInstance(slave);
            if (kubernetesComputer != null) {
                return kubernetesComputer;
            }
        }
        return new ArmadaComputer(slave);
    }

    /**
     * Creates a new instance of {@link ArmadaComputer}.
     * @return a new instance of {@link ArmadaComputer}.
     */
    public abstract ArmadaComputer newInstance(ArmadaSlave slave);
}
