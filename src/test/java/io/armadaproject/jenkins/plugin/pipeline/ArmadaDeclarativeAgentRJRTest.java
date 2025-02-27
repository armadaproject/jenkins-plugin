/*
 * Copyright 2024 CloudBees, Inc.
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

import java.net.UnknownHostException;
import io.armadaproject.jenkins.plugin.pipeline.steps.AssertBuildStatusSuccess;
import io.armadaproject.jenkins.plugin.pipeline.steps.RunId;
import io.armadaproject.jenkins.plugin.pipeline.steps.SetupCloud;
import org.junit.Test;

public final class ArmadaDeclarativeAgentRJRTest extends AbstractKubernetesPipelineRJRTest {

    public ArmadaDeclarativeAgentRJRTest() throws UnknownHostException {
        super(new SetupCloud());
    }

    @Test
    public void declarative() throws Throwable {
        RunId runId = createWorkflowJobThenScheduleRun();
        rjr.runRemotely(new AssertBuildStatusSuccess(runId));
    }
}
