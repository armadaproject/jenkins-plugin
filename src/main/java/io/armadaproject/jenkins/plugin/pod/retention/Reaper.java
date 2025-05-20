/*
 * Copyright 2019 CloudBees, Inc.
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

package io.armadaproject.jenkins.plugin.pod.retention;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.EphemeralNode;
import hudson.slaves.OfflineCause;
import io.armadaproject.jenkins.plugin.ArmadaComputer;
import io.armadaproject.jenkins.plugin.ArmadaSlave;
import io.armadaproject.jenkins.plugin.PodUtils;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.Watcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.util.SystemProperties;
import jenkins.util.Timer;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;

/**
 * Checks for deleted pods corresponding to {@link ArmadaSlave} and ensures the node is removed from Jenkins too.
 * <p>If the pod has been deleted, all of the associated state (running user processes, workspace, etc.) must also be gone;
 * so there is no point in retaining this agent definition any further.
 * ({@link ArmadaSlave} is not an {@link EphemeralNode}: it <em>does</em> support running across Jenkins restarts.)
 * <p>Note that pod retention policies other than the default {@link Never} may disable this system,
 * unless some external process or garbage collection policy results in pod deletion.
 */
@Extension
public class Reaper extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(Reaper.class.getName());


    public static Reaper getInstance() {
        return ExtensionList.lookupSingleton(Reaper.class);
    }

    /**
     * Activate this feature only if and when some Kubernetes agent is actually used.
     * Avoids touching the API server when this plugin is not even in use.
     */
    private final AtomicBoolean activated = new AtomicBoolean();

    private final LoadingCache<String, Set<String>> terminationReasons =
            Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build(k -> new ConcurrentSkipListSet<>());

    @Override
    public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        if (c instanceof ArmadaComputer) {
            Timer.get().schedule(this::maybeActivate, 10, TimeUnit.SECONDS);
        }
    }

    public void maybeActivate() {
        if (activated.compareAndSet(false, true)) {
            activate();
        }
    }

    private void activate() {
        LOGGER.fine("Activating reaper");
        // First check all existing nodes to see if they still have active pods.
        // (We may have missed deletion events while Jenkins was shut off,
        // or pods may have been deleted before any Kubernetes agent was brought online.)
        reapAgents();
    }

    /**
     * Remove any {@link ArmadaSlave} nodes that reference Pods that don't exist.
     */
    private void reapAgents() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            for (Node n : new ArrayList<>(jenkins.getNodes())) {
                if (!(n instanceof ArmadaSlave)) {
                    continue;
                }
                ArmadaSlave ks = (ArmadaSlave) n;
                if (ks.getLauncher().isLaunchSupported()) {
                    // Being launched, don't touch it.
                    continue;
                }
                String ns = ks.getNamespace();
                String name = ks.getPodName();
                try {
                    // TODO more efficient to do a single (or paged) list request, but tricky since there may be
                    // multiple clouds,
                    // and even within a single cloud an agent pod is permitted to use a nondefault namespace,
                    // yet we do not want to do an unnamespaced pod list for RBAC reasons.
                    // Could use a hybrid approach: first list all pods in the configured namespace for all clouds;
                    // then go back and individually check any unmatched agents with their configured namespace.
                    if (ks.connect().pods().inNamespace(ns).withName(name).get() == null) {
                        LOGGER.info(() -> ns + "/" + name
                                + " seems to have been deleted, so removing corresponding Jenkins agent");
                        jenkins.removeNode(ks);
                    } else {
                        LOGGER.fine(() -> ns + "/" + name + " still seems to exist, OK");
                    }
                } catch (KubernetesAuthException | IOException | RuntimeException x) {
                    LOGGER.log(Level.WARNING, x, () -> "failed to do initial reap check for " + ns + "/" + name);
                }
            }
        }
    }

    private static Optional<ArmadaSlave> resolveNode(@NonNull Jenkins jenkins, String namespace, String name) {
        return new ArrayList<>(jenkins.getNodes())
                .stream()
                        .filter(ArmadaSlave.class::isInstance)
                        .map(ArmadaSlave.class::cast)
                        .filter(ks ->
                                Objects.equals(ks.getNamespace(), namespace) && Objects.equals(ks.getPodName(), name))
                        .findFirst();
    }

    /**
     * Get any reason(s) why a node was terminated by a listener.
     * @param node a {@link Node#getNodeName}
     * @return a possibly empty set of {@link ContainerStateTerminated#getReason} or {@link PodStatus#getReason}
     */
    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification =
                    "Confused by @org.checkerframework.checker.nullness.qual.Nullable on LoadingCache.get? Never null here.")
    @NonNull
    public Set<String> terminationReasons(@NonNull String node) {
        synchronized (terminationReasons) {
            return new HashSet<>(terminationReasons.get(node));
        }
    }

    /**
     * Listener called when a Kubernetes event related to a Kubernetes agent happens.
     */
    public interface Listener extends ExtensionPoint {

        /**
         * Handle Pod event.
         * @param action the kind of event that happened to the referred pod
         * @param node The affected node
         * @param pod The affected pod
         * @param terminationReasons Set of termination reasons
         */
        void onEvent(
                @NonNull Watcher.Action action,
                @NonNull ArmadaSlave node,
                @NonNull Pod pod,
                @NonNull Set<String> terminationReasons)
                throws IOException, InterruptedException;
    }

    @Extension
    public static class RemoveAgentOnPodDeleted implements Listener {
        @Override
        public void onEvent(
                @NonNull Watcher.Action action,
                @NonNull ArmadaSlave node,
                @NonNull Pod pod,
                @NonNull Set<String> terminationReasons)
                throws IOException {
            if (action != Watcher.Action.DELETED) {
                return;
            }
            String ns = pod.getMetadata().getNamespace();
            String name = pod.getMetadata().getName();
            LOGGER.info(() -> ns + "/" + name + " was just deleted, so removing corresponding Jenkins agent");
            node.getRunListener().getLogger().printf("Pod %s/%s was just deleted%n", ns, name);
            Jenkins.get().removeNode(node);
            disconnectComputer(node, new PodOfflineCause(Messages._PodOfflineCause_PodDeleted()));
        }
    }

    @Extension
    public static class TerminateAgentOnContainerTerminated implements Listener {

        @Override
        public void onEvent(
                @NonNull Watcher.Action action,
                @NonNull ArmadaSlave node,
                @NonNull Pod pod,
                @NonNull Set<String> terminationReasons)
                throws IOException, InterruptedException {
            if (action != Watcher.Action.MODIFIED) {
                return;
            }

            List<ContainerStatus> terminatedContainers = PodUtils.getTerminatedContainers(pod);
            if (!terminatedContainers.isEmpty()) {
                List<String> containers = new ArrayList<>();
                terminatedContainers.forEach(c -> {
                    ContainerStateTerminated t = c.getState().getTerminated();
                    String containerName = c.getName();
                    containers.add(containerName);
                    String reason = t.getReason();
                    if (reason != null) {
                        terminationReasons.add(reason);
                    }
                });
                String reason = pod.getStatus().getReason();
                String message = pod.getStatus().getMessage();
                var sb = new StringBuilder()
                        .append(pod.getMetadata().getNamespace())
                        .append("/")
                        .append(pod.getMetadata().getName());
                if (containers.size() > 1) {
                    sb.append(" Containers ")
                            .append(String.join(",", containers))
                            .append(" were terminated.");
                } else {
                    sb.append(" Container ")
                            .append(String.join(",", containers))
                            .append(" was terminated.");
                }
                logAndCleanUp(
                        node,
                        pod,
                        terminationReasons,
                        reason,
                        message,
                        sb,
                        node.getRunListener(),
                        new PodOfflineCause(Messages._PodOfflineCause_ContainerFailed("ContainerError", containers)));
            }
        }
    }

    @Extension
    public static class TerminateAgentOnPodFailed implements Listener {
        @Override
        public void onEvent(
                @NonNull Watcher.Action action,
                @NonNull ArmadaSlave node,
                @NonNull Pod pod,
                @NonNull Set<String> terminationReasons)
                throws IOException, InterruptedException {
            if (action != Watcher.Action.MODIFIED) {
                return;
            }

            if ("Failed".equals(pod.getStatus().getPhase())) {
                String reason = pod.getStatus().getReason();
                String message = pod.getStatus().getMessage();
                logAndCleanUp(
                        node,
                        pod,
                        terminationReasons,
                        reason,
                        message,
                        new StringBuilder()
                                .append(pod.getMetadata().getNamespace())
                                .append("/")
                                .append(pod.getMetadata().getName())
                                .append(" Pod just failed."),
                        node.getRunListener(),
                        new PodOfflineCause(Messages._PodOfflineCause_PodFailed(reason, message)));
            }
        }
    }

    private static void logAndCleanUp(
            ArmadaSlave node,
            Pod pod,
            Set<String> terminationReasons,
            String reason,
            String message,
            StringBuilder sb,
            TaskListener runListener,
            PodOfflineCause cause)
            throws IOException, InterruptedException {
        List<String> details = new ArrayList<>();
        if (reason != null) {
            details.add("Reason: " + reason);
            terminationReasons.add(reason);
        }
        if (message != null) {
            details.add("Message: " + message);
        }
        if (!details.isEmpty()) {
            sb.append(" ").append(String.join(", ", details)).append(".");
        }
        var evictionCondition = pod.getStatus().getConditions().stream()
                .filter(c -> "EvictionByEvictionAPI".equals(c.getReason()))
                .findFirst();
        if (evictionCondition.isPresent()) {
            sb.append(" Pod was evicted by the Kubernetes Eviction API.");
            terminationReasons.add(evictionCondition.get().getReason());
        }
        LOGGER.info(() -> sb + " Removing corresponding node " + node.getNodeName() + " from Jenkins.");
        runListener.getLogger().println(sb);
        logLastLinesThenTerminateNode(node, pod, runListener);
        PodUtils.cancelQueueItemFor(pod, "PodFailure");
        disconnectComputer(node, cause);
    }

    private static void logLastLinesThenTerminateNode(ArmadaSlave node, Pod pod, TaskListener runListener)
            throws IOException, InterruptedException {
        try {
            String lines = PodUtils.logLastLines(pod, node.connect());
            if (lines != null) {
                runListener.getLogger().print(lines);
            }
        } catch (KubernetesAuthException e) {
            LOGGER.log(Level.FINE, e, () -> "Unable to get logs after pod failed event");
        } finally {
            node.terminate();
        }
    }

    /**
     * Disconnect computer associated with the given node. Should be called AFTER terminate so the offline cause
     * takes precedence over the one set by {@link ArmadaSlave#terminate()} (via {@link jenkins.model.Nodes#removeNode(Node)}).
     * @see Computer#disconnect(OfflineCause)
     * @param node node to disconnect
     * @param cause reason for offline
     */
    private static void disconnectComputer(ArmadaSlave node, OfflineCause cause) {
        Computer computer = node.getComputer();
        if (computer != null) {
            computer.disconnect(cause);
        }
    }

    @Extension
    public static class TerminateAgentOnImagePullBackOff implements Listener {

        @SuppressFBWarnings(
                value = "MS_SHOULD_BE_FINAL",
                justification = "Allow tests or groovy console to change the value")
        public static long BACKOFF_EVENTS_LIMIT =
                SystemProperties.getInteger(Reaper.class.getName() + ".backoffEventsLimit", 3);

        public static final String IMAGE_PULL_BACK_OFF = "ImagePullBackOff";

        // For each pod with at least 1 backoff, keep track of the first backoff event for 15 minutes.
        private Cache<String, Integer> ttlCache =
                Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

        @Override
        public void onEvent(
                @NonNull Watcher.Action action,
                @NonNull ArmadaSlave node,
                @NonNull Pod pod,
                @NonNull Set<String> terminationReasons)
                throws IOException, InterruptedException {
            if (action != Watcher.Action.MODIFIED) {
                return;
            }

            List<ContainerStatus> backOffContainers = PodUtils.getContainers(pod, cs -> {
                ContainerStateWaiting waiting = cs.getState().getWaiting();
                return waiting != null
                        && waiting.getMessage() != null
                        && waiting.getMessage().contains("Back-off pulling image");
            });

            if (!backOffContainers.isEmpty()) {
                List<String> images = new ArrayList<>();
                backOffContainers.forEach(cs -> images.add(cs.getImage()));
                var podUid = pod.getMetadata().getUid();
                var backOffNumber = ttlCache.get(podUid, k -> 0);
                ttlCache.put(podUid, ++backOffNumber);
                if (backOffNumber >= BACKOFF_EVENTS_LIMIT) {
                    var imagesString = String.join(",", images);
                    node.getRunListener()
                            .error("Unable to pull container image \"" + imagesString
                                    + "\". Check if image tag name is spelled correctly.");
                    terminationReasons.add(IMAGE_PULL_BACK_OFF);
                    PodUtils.cancelQueueItemFor(pod, IMAGE_PULL_BACK_OFF);
                    node.terminate();
                    disconnectComputer(
                            node,
                            new PodOfflineCause(
                                    Messages._PodOfflineCause_ImagePullBackoff(IMAGE_PULL_BACK_OFF, images)));
                } else {
                    node.getRunListener()
                            .error("Image pull backoff detected, waiting for image to be available. Will wait for "
                                    + (BACKOFF_EVENTS_LIMIT - backOffNumber)
                                    + " more events before terminating the node.");
                }
            }
        }
    }
}
