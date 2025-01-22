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

import api.EventOuterClass.EventMessage;
import api.EventOuterClass.JobSetRequest;
import hudson.AbortException;
import hudson.model.Node;
import io.armadaproject.ArmadaClient;
import io.armadaproject.ClusterConfigParser;
import io.armadaproject.jenkins.plugin.ArmadaCloud;
import io.armadaproject.jenkins.plugin.KubernetesSlave;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import org.jenkinsci.plugins.workflow.steps.StepContext;

/**
 * helper class for steps running in a kubernetes `node` context
 */
class KubernetesNodeContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private StepContext context;

    private String podName;
    private String namespace;

    KubernetesNodeContext(StepContext context) throws Exception {
        this.context = context;
        KubernetesSlave agent = getKubernetesSlave();
        this.podName = agent.getPodName();
        this.namespace = agent.getNamespace();
    }

    // TODO remove the Exception thrown
    String getPodName() throws Exception {
        return podName;
    }

    // TODO remove the Exception thrown
    public String getNamespace() throws Exception {
        return namespace;
    }

    KubernetesClient connectToCloud() throws Exception {
        KubernetesSlave kubernetesSlave = getKubernetesSlave();
        ArmadaCloud kubernetesCloud = kubernetesSlave.getKubernetesCloud();
        AtomicReference<String> serverUrl = new AtomicReference<>();
        try (ArmadaClient armadaClient = kubernetesCloud.connectToArmada()) {
            JobSetRequest jobSetRequest = JobSetRequest.newBuilder()
                .setId(kubernetesCloud.getArmadaJobSetPrefix()
                    + kubernetesCloud.getArmadaJobSetId())
                .setQueue(kubernetesCloud.getArmadaQueue())
                .setErrorIfMissing(true)
                .build();

            armadaClient.getEvents(jobSetRequest).forEachRemaining(e -> {
                EventMessage message = e.getMessage();
                if (message.getRunning().getJobId().equals(kubernetesSlave.getArmadaJobId())) {
                    String clusterId = message.getRunning().getClusterId();
                  try {
                      serverUrl.set(
                          ClusterConfigParser.parse(kubernetesCloud.getArmadaClusterConfigPath())
                              .get(clusterId));

                  } catch (Exception ex) {
                    throw new RuntimeException("Failed to parse cluster config file", ex);
                  }

                    namespace = message.getRunning().getPodNamespace();
                    podName = message.getRunning().getPodName();
                }
            });
        }

        return kubernetesCloud.connect(serverUrl.get(), namespace);
    }

    private KubernetesSlave getKubernetesSlave() throws IOException, InterruptedException {
        Node node = context.get(Node.class);
        if (!(node instanceof KubernetesSlave)) {
            throw new AbortException(
                    String.format("Node is not a Kubernetes node: %s", node != null ? node.getNodeName() : null));
        }
        return (KubernetesSlave) node;
    }
}
