/*
 * Copyright 2020 CloudBees, Inc.
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

package io.armadaproject.jenkins.plugin;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.model.Label;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import io.armadaproject.jenkins.plugin.pipeline.PodTemplateStepExecution;

public final class PodUtils {
    private PodUtils() {}

    private static final Logger LOGGER = Logger.getLogger(PodUtils.class.getName());

    public static void cancelQueueItemFor(ArmadaSlave node, String reason) {
        var queueItem = node.getItem();
        if(queueItem != null) {
            LOGGER.log(Level.FINE, "Canceling queue item \"" + queueItem.task.getDisplayName() + "\"\n" + (!StringUtils.isBlank(reason) ? "due to " + reason : ""));
            var queue = Jenkins.get().getQueue();
            queue.cancel(queueItem);
        }
    }

    public static void cancelQueueItemFor(
            @NonNull String runUrl,
            @NonNull String label,
            @CheckForNull String reason,
            @CheckForNull String podDisplayName) {
        var queue = Jenkins.get().getQueue();
        Arrays.stream(queue.getItems())
                .filter(item -> item.getTask().getUrl().equals(runUrl))
                .filter(item -> Optional.ofNullable(item.getAssignedLabel())
                        .map(Label::getName)
                        .map(name -> PodTemplateUtils.sanitizeLabel(name).equals(label))
                        .orElse(false))
                .findFirst()
                .ifPresentOrElse(
                        item -> {
                            LOGGER.log(
                                    Level.FINE,
                                    () -> "Cancelling queue item: \"" + item.task.getDisplayName() + "\"\n"
                                            + (!StringUtils.isBlank(reason) ? "due to " + reason : ""));
                            queue.cancel(item);
                        },
                        () -> {
                            if (podDisplayName != null) {
                                LOGGER.log(Level.FINE, () -> "No queue item found for pod " + podDisplayName);
                            }
                        });
    }

    @CheckForNull
    public static String logLastLines(@NonNull Pod pod, @NonNull KubernetesClient client) {
        PodStatus status = pod.getStatus();
        ObjectMeta metadata = pod.getMetadata();
        if (status == null || metadata == null) {
            return null;
        }
        String podName = metadata.getName();
        String namespace = metadata.getNamespace();
        List<ContainerStatus> containers = status.getContainerStatuses();
        StringBuilder sb = new StringBuilder();
        if (containers != null) {
            for (ContainerStatus containerStatus : containers) {
                sb.append("\n");
                sb.append("- ");
                sb.append(containerStatus.getName());
                if (containerStatus.getState().getTerminated() != null) {
                    sb.append(" -- terminated (");
                    sb.append(containerStatus.getState().getTerminated().getExitCode());
                    sb.append(")");
                }
                if (containerStatus.getState().getRunning() != null) {
                    sb.append(" -- running");
                }
                if (containerStatus.getState().getWaiting() != null) {
                    sb.append(" -- waiting");
                }
                sb.append("\n");
                try {
                    String log = client.pods()
                            .inNamespace(namespace)
                            .withName(podName)
                            .inContainer(containerStatus.getName())
                            .tailingLines(30)
                            .getLog();
                    sb.append("-----Logs-------------\n");
                    sb.append(log);
                    sb.append("\n");
                } catch (KubernetesClientException e) {
                    LOGGER.log(
                            Level.FINE,
                            e,
                            () -> namespace + "/" + podName
                                    + " Unable to retrieve container logs as the pod is already gone");
                }
            }
        }
        return Util.fixEmpty(sb.toString());
    }
}
