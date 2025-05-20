package io.armadaproject.jenkins.plugin.pod.decorator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.armadaproject.jenkins.plugin.ArmadaCloud;
import io.armadaproject.jenkins.plugin.ArmadaSlave;
import io.armadaproject.jenkins.plugin.PodTemplate;
import io.armadaproject.jenkins.plugin.PodTemplateBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class PodDecoratorTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ArmadaSlave slave;

    private ArmadaCloud cloud = new ArmadaCloud("test");

    @Before
    public void setUp() {
        when(slave.getArmadaCloud()).thenReturn(cloud);
    }

    @TestExtension("activeDecorator")
    public static class PodDecoratorImpl implements PodDecorator {
        @NonNull
        @Override
        public Pod decorate(@NonNull ArmadaCloud armadaCloud, @NonNull Pod pod) {
            // @formatter:off
            return new PodBuilder(pod)
                    .editOrNewMetadata()
                    .addToLabels("poddecoratorimpl", "true")
                    .endMetadata()
                    .build();
            // @formatter:on
        }
    }

    @Test
    public void activeDecorator() {
        PodTemplate podTemplate = new PodTemplate();
        PodTemplateBuilder podTemplateBuilder = new PodTemplateBuilder(podTemplate, slave);
        Pod pod = podTemplateBuilder.build();
        assertEquals("true", pod.getMetadata().getLabels().get("poddecoratorimpl"));
    }
}
