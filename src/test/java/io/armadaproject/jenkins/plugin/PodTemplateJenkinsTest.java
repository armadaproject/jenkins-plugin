package io.armadaproject.jenkins.plugin;

import static io.armadaproject.jenkins.plugin.PodTemplate.getLabelDigestFunction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import hudson.model.Label;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

public class PodTemplateJenkinsTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @Issue({"JENKINS-59690", "JENKINS-60537"})
    public void singleLabel() {
        PodTemplate podTemplate = new PodTemplate();
        podTemplate.setLabel("foo");
        Map<String, String> labelsMap = podTemplate.getLabelsMap();
        assertEquals("foo", labelsMap.get("jenkins/label"));
        MessageDigest labelDigestFunction = getLabelDigestFunction();
        labelDigestFunction.update("foo".getBytes(StandardCharsets.UTF_8));
        assertEquals(
                String.format("%040x", new BigInteger(1, labelDigestFunction.digest())),
                labelsMap.get("jenkins/label-digest"));
    }

    @Test
    @Issue({"JENKINS-59690", "JENKINS-60537"})
    public void multiLabel() {
        PodTemplate podTemplate = new PodTemplate();
        podTemplate.setLabel("foo bar");
        Map<String, String> labelsMap = podTemplate.getLabelsMap();
        assertEquals("foo_bar", labelsMap.get("jenkins/label"));
        MessageDigest labelDigestFunction = getLabelDigestFunction();
        labelDigestFunction.update("foo bar".getBytes(StandardCharsets.UTF_8));
        assertEquals(
                String.format("%040x", new BigInteger(1, labelDigestFunction.digest())),
                labelsMap.get("jenkins/label-digest"));
    }

    @Test
    @Issue({"JENKINS-59690", "JENKINS-60537"})
    public void defaultLabel() {
        PodTemplate podTemplate = new PodTemplate();
        podTemplate.setLabel(null);
        Map<String, String> labelsMap = podTemplate.getLabelsMap();
        assertEquals("slave-default", labelsMap.get("jenkins/label"));
        assertEquals("0", labelsMap.get("jenkins/label-digest"));
    }

    @Test
    public void jenkinsLabels() {
        ArmadaCloud armadaCloud = new ArmadaCloud("kubernetes");
        j.jenkins.clouds.add(armadaCloud);
        PodTemplate podTemplate = new PodTemplate();
        armadaCloud.addTemplate(podTemplate);
        podTemplate.setLabel("foo bar");

        Set<String> labels = j.jenkins.getLabels().stream().map(Label::getName).collect(Collectors.toSet());
        assertThat(
                labels,
                Matchers.anyOf(
                        containsInAnyOrder("master", "foo", "bar"), containsInAnyOrder("built-in", "foo", "bar")));
    }
}
