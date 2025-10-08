package io.armadaproject.jenkins.plugin;

import hudson.Util;
import hudson.model.Descriptor;
import hudson.slaves.NodeProvisioner;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * The default {@link PlannedNodeBuilder} implementation, in case there is other registered.
 */
public class StandardPlannedNodeBuilder extends PlannedNodeBuilder {
    @Override
    public NodeProvisioner.PlannedNode build() {
        ArmadaCloud cloud = getCloud();
        PodTemplate t = getTemplate();
        CompletableFuture f;
        String displayName;
        try {
            ArmadaSlave agent = ArmadaSlave.builder()
                    .podTemplate(t.isUnwrapped() ? t : cloud.getUnwrappedTemplate(t))
                    .cloud(cloud)
                    .build();
            displayName = agent.getDisplayName();
            f = CompletableFuture.completedFuture(agent);
        } catch (IOException | Descriptor.FormException e) {
            displayName = null;
            f = new CompletableFuture();
            f.completeExceptionally(e);
        }
        return new NodeProvisioner.PlannedNode(Util.fixNull(displayName), f, getNumExecutors());
    }
}
