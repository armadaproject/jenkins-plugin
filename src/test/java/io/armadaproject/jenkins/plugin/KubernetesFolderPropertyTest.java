package io.armadaproject.jenkins.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;

import com.cloudbees.hudson.plugins.folder.Folder;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class KubernetesFolderPropertyTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void propertySavedOnFirstSaveTest() throws Exception {
        ArmadaCloud kube1 = new ArmadaCloud("kube1");
        kube1.setUsageRestricted(true);
        ArmadaCloud kube2 = new ArmadaCloud("kube2");
        kube2.setUsageRestricted(true);
        j.jenkins.clouds.add(kube1);
        j.jenkins.clouds.add(kube2);

        Folder folder = j.jenkins.createProject(Folder.class, "folder001");
        ArmadaFolderProperty prop = new ArmadaFolderProperty();
        folder.addProperty(prop);

        Folder after = j.configRoundtrip(folder);
        assertThat(
                "Property exists after saving",
                after.getProperties().get(ArmadaFolderProperty.class),
                notNullValue());
        assertThat(
                "No selected clouds",
                after.getProperties().get(ArmadaFolderProperty.class).getPermittedClouds(),
                empty());

        folder.getProperties()
                .get(ArmadaFolderProperty.class)
                .setPermittedClouds(Collections.singletonList("kube1"));
        after = j.configRoundtrip(folder);
        assertThat(
                "Kube1 cloud is added",
                after.getProperties().get(ArmadaFolderProperty.class).getPermittedClouds(),
                contains("kube1"));

        Folder subFolder = folder.createProject(Folder.class, "subfolder001");
        ArmadaFolderProperty prop2 = new ArmadaFolderProperty();
        prop2.setPermittedClouds(Collections.singletonList("kube2"));
        subFolder.addProperty(prop2);

        after = j.configRoundtrip(subFolder);
        assertThat(
                "Contains own and inherited cloud",
                after.getProperties().get(ArmadaFolderProperty.class).getPermittedClouds(),
                containsInAnyOrder("kube1", "kube2"));
    }
}
