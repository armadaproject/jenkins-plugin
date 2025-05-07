package io.armadaproject.jenkins.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import io.armadaproject.jenkins.plugin.pod.retention.Always;
import io.armadaproject.jenkins.plugin.pod.retention.PodRetention;
import io.armadaproject.jenkins.plugin.volumes.EmptyDirVolume;
import io.armadaproject.jenkins.plugin.volumes.PodVolume;
import io.armadaproject.jenkins.plugin.volumes.workspace.WorkspaceVolume;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class ArmadaCloudTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public LoggerRule logs = new LoggerRule()
            .record(Logger.getLogger(ArmadaCloud.class.getPackage().getName()), Level.ALL);

    @After
    public void tearDown() {
        System.getProperties().remove("KUBERNETES_JENKINS_URL");
    }

    @Test
    public void configRoundTrip() throws Exception {
        var cloud = new ArmadaCloud("kubernetes");
        var podTemplate = new PodTemplate();
        podTemplate.setName("test-template");
        podTemplate.setLabel("test");
        cloud.addTemplate(podTemplate);
        var jenkins = j.jenkins;
        jenkins.clouds.add(cloud);
        jenkins.save();
        j.submit(j.createWebClient().goTo("cloud/kubernetes/configure").getFormByName("config"));
        assertEquals(cloud, jenkins.clouds.get(ArmadaCloud.class));
    }

    @Test
    public void testInheritance() {

        ContainerTemplate jnlp = new ContainerTemplate("jnlp", "jnlp:1");
        ContainerTemplate maven = new ContainerTemplate("maven", "maven:1");
        maven.setTtyEnabled(true);
        maven.setCommand("cat");

        PodVolume podVolume = new EmptyDirVolume("/some/path", true);
        PodTemplate parent = new PodTemplate();
        parent.setName("parent");
        parent.setLabel("parent");
        parent.setContainers(Arrays.asList(jnlp));
        parent.setVolumes(Arrays.asList(podVolume));

        ContainerTemplate maven2 = new ContainerTemplate("maven", "maven:2");
        PodTemplate withNewMavenVersion = new PodTemplate();
        withNewMavenVersion.setContainers(Arrays.asList(maven2));

        PodTemplate result = PodTemplateUtils.combine(parent, withNewMavenVersion);
    }

    @Test(expected = IllegalStateException.class)
    public void getJenkinsUrlOrDie_NoJenkinsUrl() {
        JenkinsLocationConfiguration.get().setUrl(null);
        ArmadaCloud cloud = new ArmadaCloud("name");
        String url = cloud.getJenkinsUrlOrDie();
        fail("Should have thrown IllegalStateException at this point but got " + url + " instead.");
    }

    @Test
    public void getJenkinsUrlOrDie_UrlInCloud() {
        System.setProperty("KUBERNETES_JENKINS_URL", "http://mylocationinsysprop");
        ArmadaCloud cloud = new ArmadaCloud("name");
        cloud.setJenkinsUrl("http://mylocation");
        assertEquals("http://mylocation/", cloud.getJenkinsUrlOrDie());
    }

    @Test
    public void getJenkinsUrlOrDie_UrlInSysprop() {
        System.setProperty("KUBERNETES_JENKINS_URL", "http://mylocation");
        ArmadaCloud cloud = new ArmadaCloud("name");
        assertEquals("http://mylocation/", cloud.getJenkinsUrlOrDie());
    }

    @Test
    public void getJenkinsUrlOrDie_UrlInLocation() {
        JenkinsLocationConfiguration.get().setUrl("http://mylocation");
        ArmadaCloud cloud = new ArmadaCloud("name");
        assertEquals("http://mylocation/", cloud.getJenkinsUrlOrDie());
    }

    @Test
    public void getJenkinsUrlOrNull_NoJenkinsUrl() {
        JenkinsLocationConfiguration.get().setUrl(null);
        ArmadaCloud cloud = new ArmadaCloud("name");
        String url = cloud.getJenkinsUrlOrNull();
        assertNull(url);
    }

    @Test
    public void getJenkinsUrlOrNull_UrlInCloud() {
        System.setProperty("KUBERNETES_JENKINS_URL", "http://mylocationinsysprop");
        ArmadaCloud cloud = new ArmadaCloud("name");
        cloud.setJenkinsUrl("http://mylocation");
        assertEquals("http://mylocation/", cloud.getJenkinsUrlOrNull());
    }

    @Test
    public void getJenkinsUrlOrNull_UrlInSysprop() {
        System.setProperty("KUBERNETES_JENKINS_URL", "http://mylocation");
        ArmadaCloud cloud = new ArmadaCloud("name");
        assertEquals("http://mylocation/", cloud.getJenkinsUrlOrNull());
    }

    @Test
    public void getJenkinsUrlOrNull_UrlInLocation() {
        JenkinsLocationConfiguration.get().setUrl("http://mylocation");
        ArmadaCloud cloud = new ArmadaCloud("name");
        assertEquals("http://mylocation/", cloud.getJenkinsUrlOrNull());
    }

    @Test
    public void testKubernetesCloudDefaults() {
        ArmadaCloud cloud = new ArmadaCloud("name");
        assertEquals(PodRetention.getKubernetesCloudDefault(), cloud.getPodRetention());
    }

    @Test
    public void testPodLabels() {
        List<PodLabel> defaultPodLabelsList = PodLabel.fromMap(ArmadaCloud.DEFAULT_POD_LABELS);
        ArmadaCloud cloud = new ArmadaCloud("name");
        assertEquals(ArmadaCloud.DEFAULT_POD_LABELS, cloud.getPodLabelsMap());
        assertEquals(defaultPodLabelsList, cloud.getPodLabels());
        assertEquals(cloud.getPodLabelsMap(), cloud.getLabels());

        List<PodLabel> labels = PodLabel.listOf("foo", "bar", "cat", "dog");
        cloud.setPodLabels(labels);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("foo", "bar");
        expected.put("cat", "dog");
        assertEquals(expected, cloud.getPodLabelsMap());
        assertEquals(cloud.getPodLabelsMap(), cloud.getLabels());
        assertEquals(new ArrayList<>(labels), cloud.getPodLabels());

        cloud.setPodLabels(null);
        assertEquals(ArmadaCloud.DEFAULT_POD_LABELS, cloud.getPodLabelsMap());
        assertEquals(defaultPodLabelsList, cloud.getPodLabels());

        cloud.setPodLabels(new ArrayList<>());
        assertEquals(ArmadaCloud.DEFAULT_POD_LABELS, cloud.getPodLabelsMap());
        assertEquals(cloud.getPodLabelsMap(), cloud.getLabels());
        assertEquals(defaultPodLabelsList, cloud.getPodLabels());
    }

    @Test
    public void testLabels() {
        ArmadaCloud cloud = new ArmadaCloud("name");

        List<PodLabel> labels = PodLabel.listOf("foo", "bar", "cat", "dog");
        cloud.setPodLabels(labels);
        Map<String, String> labelsMap = new LinkedHashMap<>();
        for (PodLabel l : labels) {
            labelsMap.put(l.getKey(), l.getValue());
        }
        cloud.setLabels(labelsMap);
        assertEquals(new LinkedHashMap<>(labelsMap), cloud.getPodLabelsMap());
        assertEquals(labels, cloud.getPodLabels());

        cloud.setLabels(null);
        assertEquals(Collections.singletonMap("jenkins", "slave"), cloud.getPodLabelsMap());
        assertEquals(Collections.singletonMap("jenkins", "slave"), cloud.getLabels());

        cloud.setLabels(new LinkedHashMap<>());
        assertEquals(Collections.singletonMap("jenkins", "slave"), cloud.getPodLabelsMap());
        assertEquals(Collections.singletonMap("jenkins", "slave"), cloud.getLabels());
    }

    @Test
    public void copyConstructor() throws Exception {
        PodTemplate pt = new PodTemplate();
        pt.setName("podTemplate");

        ArmadaCloud cloud = new ArmadaCloud("name");
        var objectProperties =
                Set.of("templates", "podRetention", "podLabels", "labels", "serverCertificate", "garbageCollection");
        for (String property : PropertyUtils.describe(cloud).keySet()) {
            if (PropertyUtils.isWriteable(cloud, property)) {
                Class<?> propertyType = PropertyUtils.getPropertyType(cloud, property);
                if (propertyType == String.class) {
                    if (property.endsWith("Str")) {
                        // setContainerCapStr
                        // setMaxRequestsPerHostStr
                        PropertyUtils.setProperty(cloud, property, RandomStringUtils.randomNumeric(3));
                    } else {
                        PropertyUtils.setProperty(cloud, property, RandomStringUtils.randomAlphabetic(10));
                    }
                } else if (propertyType == int.class) {
                    PropertyUtils.setProperty(cloud, property, RandomUtils.nextInt());
                } else if (propertyType == Integer.class) {
                    PropertyUtils.setProperty(cloud, property, Integer.valueOf(RandomUtils.nextInt()));
                } else if (propertyType == boolean.class) {
                    PropertyUtils.setProperty(cloud, property, RandomUtils.nextBoolean());
                } else if (!objectProperties.contains(property)) {
                    fail("Unhandled field in copy constructor: " + property);
                }
            }
        }
        cloud.setServerCertificate("-----BEGIN CERTIFICATE-----");
        cloud.setTemplates(Collections.singletonList(pt));
        cloud.setPodRetention(new Always());
        cloud.setPodLabels(PodLabel.listOf("foo", "bar", "cat", "dog"));
        cloud.setLabels(Collections.singletonMap("foo", "bar"));

        ArmadaCloud copy = new ArmadaCloud("copy", cloud);
        assertEquals("copy", copy.name);
        assertTrue(
                "Expected cloud from copy constructor to be equal to the source except for name",
                EqualsBuilder.reflectionEquals(cloud, copy, true, ArmadaCloud.class, "name"));
    }

    @Test
    public void defaultWorkspaceVolume() throws Exception {
        ArmadaCloud cloud = new ArmadaCloud("kubernetes");
        j.jenkins.clouds.add(cloud);
        j.jenkins.save();
        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage p = wc.goTo("cloud/kubernetes/new");
        HtmlForm f = p.getFormByName("config");
        HtmlInput templateName = getInputByName(f, "_.name");
        templateName.setValue("default-workspace-volume");
        j.submit(f);
        cloud = j.jenkins.clouds.get(ArmadaCloud.class);
        PodTemplate podTemplate = cloud.getTemplates().get(0);
        assertEquals("default-workspace-volume", podTemplate.getName());
        assertEquals(WorkspaceVolume.getDefault(), podTemplate.getWorkspaceVolume());
        // test whether we can edit a template
        p = wc.goTo("cloud/kubernetes/template/" + podTemplate.getId() + "/");
        f = p.getFormByName("config");
        templateName = getInputByName(f, "_.name");
        templateName.setValue("default-workspace");
        j.submit(f);
        podTemplate = cloud.getTemplates().get(0);
        assertEquals("default-workspace", podTemplate.getName());
        p = wc.goTo("cloud/kubernetes/templates");
        DomElement row = p.getElementById("template_" + podTemplate.getId());
        assertTrue(row != null);
    }

    @Test
    public void minRetentionTimeout() {
        ArmadaCloud cloud = new ArmadaCloud("kubernetes");
        assertEquals(ArmadaCloud.DEFAULT_RETENTION_TIMEOUT_MINUTES, cloud.getRetentionTimeout());
        cloud.setRetentionTimeout(0);
        assertEquals(ArmadaCloud.DEFAULT_RETENTION_TIMEOUT_MINUTES, cloud.getRetentionTimeout());
    }

    @Test
    @LocalData
    public void emptyKubernetesCloudReadResolve() {
        ArmadaCloud cloud = j.jenkins.clouds.get(ArmadaCloud.class);
        assertEquals(ArmadaCloud.DEFAULT_RETENTION_TIMEOUT_MINUTES, cloud.getRetentionTimeout());
        assertEquals(Integer.MAX_VALUE, cloud.getContainerCap());
        assertEquals(ArmadaCloud.DEFAULT_MAX_REQUESTS_PER_HOST, cloud.getMaxRequestsPerHost());
        assertEquals(PodRetention.getKubernetesCloudDefault(), cloud.getPodRetention());
        assertEquals(ArmadaCloud.DEFAULT_WAIT_FOR_POD_SEC, cloud.getWaitForPodSec());
    }

    @Test
    @LocalData
    public void readResolveContainerCapZero() {
        ArmadaCloud cloud = j.jenkins.clouds.get(ArmadaCloud.class);
        assertEquals(cloud.getContainerCap(), Integer.MAX_VALUE);
    }

    public HtmlInput getInputByName(DomElement root, String name) {
        DomNodeList<HtmlElement> inputs = root.getElementsByTagName("input");
        for (HtmlElement input : inputs) {
            if (name.equals(input.getAttribute("name"))) {
                return (HtmlInput) input;
            }
        }
        return null;
    }

    @Test
    public void testArmadaJobSetIdRotation() {
        ArmadaCloud armadaCloud = new ArmadaCloud("test");

        String jobSetId = new SimpleDateFormat("-ddMMyyyy").format(new Date());
        armadaCloud.setArmadaJobSetId(jobSetId);

        assertEquals(jobSetId, armadaCloud.getCompleteArmadaJobSetId());

        String newJobSetId = new SimpleDateFormat("-ddMMyyyy").format(
            new Date(System.currentTimeMillis() + 86400000L)); // Add 1 day
        armadaCloud.setArmadaJobSetId(newJobSetId);

        assertEquals(newJobSetId, armadaCloud.getCompleteArmadaJobSetId());
    }

    @Test
    public void testArmadaJobSetIdRotationWithPrefix() {
        ArmadaCloud armadaCloud = new ArmadaCloud("test");

        String jobSetId = new SimpleDateFormat("-ddMMyyyy").format(new Date());
        armadaCloud.setArmadaJobSetId(jobSetId);
        String jobSetIdPrefix = "prefix";
        armadaCloud.setArmadaJobSetPrefix("prefix");

        String expectedJobSetId = jobSetIdPrefix + "-" + jobSetId;
        assertEquals(expectedJobSetId, armadaCloud.getCompleteArmadaJobSetId());

        String newJobSetId = new SimpleDateFormat("-ddMMyyyy").format(
            new Date(System.currentTimeMillis() + 86400000L)); // Add 1 day
        armadaCloud.setArmadaJobSetId(newJobSetId);

        expectedJobSetId = jobSetIdPrefix + "-" + newJobSetId;
        assertEquals(expectedJobSetId, armadaCloud.getCompleteArmadaJobSetId());
    }
}
