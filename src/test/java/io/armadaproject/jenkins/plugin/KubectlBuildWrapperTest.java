package io.armadaproject.jenkins.plugin;

import static io.armadaproject.jenkins.plugin.KubernetesTestUtil.getLabels;
import static org.junit.Assert.assertNotNull;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Result;
import io.armadaproject.jenkins.plugin.pipeline.AbstractKubernetesPipelineTest;
import org.junit.Before;
import org.junit.Test;

public class KubectlBuildWrapperTest extends AbstractKubernetesPipelineTest {
    @Before
    public void setUp() throws Exception {
        KubernetesTestUtil.deletePods(cloud.connect(), KubernetesTestUtil.getLabels(cloud, this, name), false);
        assertNotNull(createJobThenScheduleRun());
        UsernamePasswordCredentialsImpl creds = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "id", "some credentials", "username", "password");
        SystemCredentialsProvider.getInstance().getCredentials().add(creds);
    }

    @Test
    public void kubectlBuildWrapper_missingCredentials() throws Exception {
        r.assertBuildStatus(Result.FAILURE, r.waitForCompletion(b));
        r.assertLogContains("No credentials found for id \"abcd\"", b);
    }

    @Test
    public void kubectlBuildWrapper_invalidCredentials() throws Exception {
        r.assertBuildStatus(Result.FAILURE, r.waitForCompletion(b));
        r.assertLogContains("Unable to connect to the server", b);
    }
}
