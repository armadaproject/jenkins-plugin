package io.armadaproject.jenkins.plugin;

import static java.util.stream.Collectors.toList;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.util.Collection;
import java.util.List;

/**
 * A source of pod templates.
 */
public abstract class PodTemplateSource implements ExtensionPoint {
    public static List<PodTemplate> getAll(@NonNull ArmadaCloud cloud) {
        return ExtensionList.lookup(PodTemplateSource.class).stream()
                .map(s -> s.getList(cloud))
                .flatMap(Collection::stream)
                .collect(toList());
    }

    /**
     * The list of {@link PodTemplate} contributed by this implementation.
     * @return The list of {@link PodTemplate} contributed by this implementation.
     * @param cloud
     */
    @NonNull
    protected abstract List<PodTemplate> getList(@NonNull ArmadaCloud cloud);
}
