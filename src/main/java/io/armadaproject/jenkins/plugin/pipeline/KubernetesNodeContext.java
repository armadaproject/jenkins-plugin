/*
 * Copyright (C) 2017 Original Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.armadaproject.jenkins.plugin.pipeline;

import hudson.AbortException;
import hudson.model.Node;
import io.armadaproject.jenkins.plugin.ArmadaCloud;
import io.armadaproject.jenkins.plugin.ArmadaSlave;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.io.Serializable;

import org.jenkinsci.plugins.workflow.steps.StepContext;

/**
 * helper class for steps running in a kubernetes `node` context
 */
class KubernetesNodeContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private StepContext context;

    private String podName;

    KubernetesNodeContext(StepContext context) throws Exception {
        this.context = context;
        ArmadaSlave agent = getKubernetesSlave();
        this.podName = agent.getPodName();
    }

    // TODO remove the Exception thrown
    String getPodName() throws Exception {
        return podName;
    }

    // TODO remove the Exception thrown
    public String getNamespace() throws Exception {
        ArmadaSlave kubernetesSlave = getKubernetesSlave();
        ArmadaCloud armadaCloud = kubernetesSlave.getArmadaCloud();
        return armadaCloud.getArmadaNamespace();
    }

    KubernetesClient connectToCloud() throws Exception {
        return getKubernetesSlave().connect();
    }

    private ArmadaSlave getKubernetesSlave() throws IOException, InterruptedException {
        Node node = context.get(Node.class);
        if (!(node instanceof ArmadaSlave)) {
            throw new AbortException(
                    String.format("Node is not a Armada node: %s", node != null ? node.getNodeName() : null));
        }
        return (ArmadaSlave) node;
    }
}
