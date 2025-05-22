/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.armadaproject.jenkins.plugin;

import java.util.Collections;
import java.util.List;
import io.armadaproject.jenkins.plugin.volumes.PodVolume;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

/**
 * @author Carlos Sanchez
 */
public class KubernetesSlaveTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @WithoutJenkins
    @Test
    public void testGetSlaveName() {
        List<? extends PodVolume> volumes = Collections.emptyList();
        List<ContainerTemplate> containers = Collections.emptyList();

        KubernetesTestUtil.assertRegex(ArmadaSlave.getSlaveName(new PodTemplate("image", volumes)), "^jenkins-agent-[0-9a-z]{5}$");
        KubernetesTestUtil.assertRegex(
                ArmadaSlave.getSlaveName(new PodTemplate("", volumes, containers)), "^jenkins-agent-[0-9a-z]{5}$");
        KubernetesTestUtil.assertRegex(
                ArmadaSlave.getSlaveName(new PodTemplate("a name", volumes, containers)), ("^a-name-[0-9a-z]{5}$"));
        KubernetesTestUtil.assertRegex(
                ArmadaSlave.getSlaveName(new PodTemplate("an_other_name", volumes, containers)),
                ("^an-other-name-[0-9a-z]{5}$"));
        KubernetesTestUtil.assertRegex(
                ArmadaSlave.getSlaveName(new PodTemplate("whatever...", volumes, containers)),
                ("jenkins-agent-[0-9a-z]{5}"));
    }
}
