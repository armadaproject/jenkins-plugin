package io.armadaproject.jenkins.plugin.pipeline;

import static io.armadaproject.jenkins.plugin.KubernetesTestUtil.testingNamespace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import io.armadaproject.jenkins.plugin.ArmadaComputer;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.Test;

public class KubernetesPipelineOverridenNamespaceTest extends AbstractKubernetesPipelineTest {

    @Test
    public void runWithCloudOverriddenNamespace() throws Exception {
        String overriddenNamespace = testingNamespace + "-overridden-namespace";
        cloud.setNamespace(overriddenNamespace);
        // Run in our own testing namespace
        createNamespaceIfNotExist(cloud.connect(), overriddenNamespace);

        assertNotNull(createJobThenScheduleRun());
        SemaphoreStep.waitForStart("pod/1", b);
        for (ArmadaComputer c : getKubernetesComputers()) {
            assertEquals(
                    overriddenNamespace,
                    c.getNode().getPod().get().getMetadata().getNamespace());
        }
        SemaphoreStep.success("pod/1", null);

        r.assertBuildStatusSuccess(r.waitForCompletion(b));
        r.assertLogContains(overriddenNamespace, b);
    }

    /**
     * Step namespace should have priority over anything else.
     */
    @Test
    public void runWithStepOverriddenNamespace() throws Exception {
        String overriddenNamespace = testingNamespace + "-overridden-namespace";
        String stepNamespace = testingNamespace + "-overridden-namespace2";
        cloud.setNamespace(overriddenNamespace);
        // Run in our own testing namespace
        createNamespaceIfNotExist(cloud.connect(), stepNamespace);

        Map<String, String> env = new HashMap<>();
        env.put("OVERRIDDEN_NAMESPACE", stepNamespace);
        assertNotNull(createJobThenScheduleRun(env));
        SemaphoreStep.waitForStart("pod/1", b);
        for (ArmadaComputer c : getKubernetesComputers()) {
            assertEquals(stepNamespace, c.getNode().getPod().get().getMetadata().getNamespace());
        }
        SemaphoreStep.success("pod/1", null);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));
        r.assertLogContains(stepNamespace, b);
    }
}
