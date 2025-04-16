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

import static org.awaitility.Awaitility.await;

import api.EventOuterClass.JobRunningEvent;
import hudson.AbortException;
import hudson.model.Node;
import io.armadaproject.ClusterConfigParser;
import io.armadaproject.jenkins.plugin.ArmadaCloud;
import io.armadaproject.jenkins.plugin.ArmadaEventManager;
import io.armadaproject.jenkins.plugin.KubernetesSlave;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
        ArmadaCloud armadaCloud = kubernetesSlave.getKubernetesCloud();

        ArmadaEventManager<JobRunningEvent> armadaEventManager =
            armadaCloud.getArmadaEventManager();
        AtomicReference<JobRunningEvent> matchedEvent = new AtomicReference<>();
        Consumer<JobRunningEvent> consumer = event -> {
            if (event.getJobId().equals(kubernetesSlave.getArmadaJobId())) {
                matchedEvent.set(event);
            }
        };
        armadaEventManager.subscribe(kubernetesSlave.getArmadaJobSetId(), consumer);

        // start watching armada events if watcher thread does not exist in cloud instance
        armadaCloud.getJobSetIdThreads().putIfAbsent(kubernetesSlave.getArmadaJobSetId(),
            armadaCloud.startWatchingArmadaEvents(kubernetesSlave.getArmadaJobSetId()));

        try {
            // Wait for the event to be recorded
            await().atMost(60, TimeUnit.SECONDS).until(() -> matchedEvent.get() != null);
        } catch (Exception e) {
            // if the event is not found, swallow exception
        }
        armadaEventManager.unsubscribe(kubernetesSlave.getArmadaJobSetId(), consumer);

        JobRunningEvent event = matchedEvent.get();
        if (event == null) {
            throw new RuntimeException("Failed to find job: " + kubernetesSlave.getArmadaJobId() +
                " running event for jobSetId: " + armadaCloud.getCompleteArmadaJobSetId());
        }

        AtomicReference<String> serverUrl = new AtomicReference<>();
        try {
            serverUrl.set(
                ClusterConfigParser.parse(armadaCloud.getArmadaClusterConfigPath())
                    .get(event.getClusterId()));

        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse cluster config file", ex);
        }

        namespace = event.getPodNamespace();
        podName = event.getPodName();

        return armadaCloud.connect(serverUrl.get(), namespace);
    }

    private KubernetesSlave getKubernetesSlave() throws IOException, InterruptedException {
        Node node = context.get(Node.class);
        if (!(node instanceof KubernetesSlave)) {
            throw new AbortException(
                    String.format("Node is not a Armada node: %s", node != null ? node.getNodeName() : null));
        }
        return (KubernetesSlave) node;
    }
}
