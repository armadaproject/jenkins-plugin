package io.armadaproject.jenkins.plugin.pod.yaml;

import static io.armadaproject.jenkins.plugin.PodTemplateUtils.parseFromYaml;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.List;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class Overrides extends YamlMergeStrategy {
    private static final long serialVersionUID = 4510341864619338164L;

    @DataBoundConstructor
    public Overrides() {}

    @Override
    public Pod merge(List<String> yamls) {
        if (yamls.isEmpty()) {
            return null;
        } else {
            return parseFromYaml(yamls.get(yamls.size() - 1));
        }
    }

    @Override
    public String toString() {
        return "Override";
    }

    @Extension
    @Symbol("override")
    public static class DescriptorImpl extends Descriptor<YamlMergeStrategy> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Override";
        }
    }
}
