/*
 * Copyright 2023 CloudBees, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.armadaproject.jenkins.plugin.pipeline;

import hudson.TcpSlaveAgentListener;
import java.util.logging.Level;
import io.armadaproject.jenkins.plugin.KubernetesTestUtil;
import io.armadaproject.jenkins.plugin.PodTemplateBuilder;
import org.junit.Before;
import org.junit.Test;

public final class DirectConnectionTest extends AbstractKubernetesPipelineTest {

    static {
        System.setProperty(
                TcpSlaveAgentListener.class.getName() + ".hostName", System.getProperty("jenkins.host.address"));
    }

    @Before
    public void setUp() throws Exception {
        KubernetesTestUtil.deletePods(cloud.connect(), KubernetesTestUtil.getLabels(cloud, this, name), false);
        cloud.setDirectConnection(true);
        logs.record(PodTemplateBuilder.class, Level.FINEST);
    }

    @Test
    public void directConnectionAgent() throws Exception {
        r.assertBuildStatusSuccess(r.waitForCompletion(createJobThenScheduleRun()));
    }
}
