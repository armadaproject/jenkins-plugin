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

import api.EventOuterClass;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.XmlFile;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.SaveableListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.EphemeralNode;
import hudson.slaves.OfflineCause;
import io.armadaproject.jenkins.plugin.ArmadaCloud;
import io.armadaproject.jenkins.plugin.ArmadaComputer;
import io.armadaproject.jenkins.plugin.ArmadaSlave;
import io.armadaproject.jenkins.plugin.PodUtils;
import io.armadaproject.jenkins.plugin.job.ArmadaClientUtil;
import io.armadaproject.jenkins.plugin.job.ArmadaEventWatcher;
import io.armadaproject.jenkins.plugin.job.ArmadaJobMetadata;
import io.armadaproject.jenkins.plugin.job.ArmadaState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.Watcher;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.util.Listeners;
import jenkins.util.SystemProperties;
import jenkins.util.Timer;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;

/**
 * Checks for deleted pods corresponding to {@link ArmadaSlave} and ensures the node is removed from Jenkins too.
 * <p>If the pod has been deleted, all of the associated state (running user processes, workspace, etc.) must also be gone;
 * so there is no point in retaining this agent definition any further.
 * ({@link ArmadaSlave} is not an {@link EphemeralNode}: it <em>does</em> support running across Jenkins restarts.)
 */
@Extension
public class Reaper extends ComputerListener {
    private static final Logger LOGGER = Logger.getLogger(Reaper.class.getName());

    @Extension
    public static class ReaperShutdownListener extends ItemListener {
        @Override
        public void onBeforeShutdown() {
            Reaper.getInstance().closeAllWatchers();
        }
    }

    public static Reaper getInstance() {
        return ExtensionList.lookupSingleton(Reaper.class);
    }

    /**
     * Activate this feature only if and when some Kubernetes agent is actually used.
     * Avoids touching the API server when this plugin is not even in use.
     */
    private final AtomicBoolean activated = new AtomicBoolean();

    private final Map<String, ArmadaJobWatcher> watchers = new ConcurrentHashMap<>();

    private final LoadingCache<String, Set<String>> terminationReasons =
            Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build(k -> new ConcurrentSkipListSet<>());

    @Override
    public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        if (c instanceof ArmadaComputer) {
            Timer.get().schedule(this::maybeActivate, 10, TimeUnit.SECONDS);

            var node = ((ArmadaComputer) c).getNode();
            if (node != null && !isWatchingCloud(node.getCloudName())) {
                try {
                    watchCloud(node.getArmadaCloud());
                } catch (IllegalStateException ise) {
                    LOGGER.log(Level.WARNING, ise, () -> "kubernetes cloud not found: " + node.getCloudName());
                }
            }
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

        // Now set up a watch for any subsequent pod deletions.
        watchClouds();
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

    private void watchClouds() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            Set<String> cloudNames = new HashSet<>(this.watchers.keySet());
            for (ArmadaCloud kc : jenkins.clouds.getAll(ArmadaCloud.class)) {
                watchCloud(kc);
                cloudNames.remove(kc.name);
            }

            // close any cloud watchers that have been removed
            cloudNames.stream().map(this.watchers::get).filter(Objects::nonNull).forEach(cpw -> {
                LOGGER.info(() -> "stopping pod watcher for deleted kubernetes cloud " + cpw.cloudName);
                cpw.stop();
            });
        }
    }

    private void watchCloud(@NonNull ArmadaCloud kc) {
        // can't use ConcurrentHashMap#computeIfAbsent because CloudPodWatcher will remove itself from the watchers
        // map on close. If an error occurs when creating the watch it would create a deadlock situation.
        var watcher = new ArmadaJobWatcher(kc);
        if (!isCloudPodWatcherActive(watcher)) {
            try {
                var jobManager = ArmadaState.getJobManager(kc);
                watcher.watch = jobManager.watchEvents(watcher);
                var old = watchers.put(kc.name, watcher);
                // if another watch slipped in then make sure it stopped
                if (old != null) {
                    old.stop();
                }
                LOGGER.info(() -> "set up watcher on " + kc.getDisplayName());
            } catch(Throwable t) {
                LOGGER.log(Level.WARNING, "Failed to set up watcher on " + kc.getDisplayName(), t);
            }
        }
    }

    boolean isWatchingCloud(String name) {
        return watchers.get(name) != null;
    }

    /**
     * Check if the given cloud pod watcher exists and is still valid. Watchers may become invalid
     * of the kubernetes client configuration changes.
     * @param watcher watcher to check
     * @return true if the provided watcher already exists and is valid, false otherwise
     */
    private boolean isCloudPodWatcherActive(@NonNull ArmadaJobWatcher watcher) {
        var existing = watchers.get(watcher.cloudName);
        return existing != null && existing.jobManagerValidity == watcher.jobManagerValidity;
    }

    private static Optional<ArmadaSlave> resolveNode(@NonNull Jenkins jenkins, String jobId, String jobSetId) {
        return new ArrayList<>(jenkins.getNodes())
                .stream()
                        .filter(ArmadaSlave.class::isInstance)
                        .map(ArmadaSlave.class::cast)
                        .filter(ks ->
                                Objects.equals(ks.getArmadaJobId(), jobId) &&
                                Objects.equals(ks.getArmadaJobSetId(), jobSetId))
                        .findFirst();
    }

    /**
     * Stop all watchers
     */
    private void closeAllWatchers() {
        // on close each watcher should remove itself from the watchers map (see ArmadaJobWatcher#onClose)
        watchers.values().forEach(ArmadaJobWatcher::stop);
    }

    private class ArmadaJobWatcher implements ArmadaEventWatcher {
        private final String cloudName;
        private final int jobManagerValidity;
        private Closeable watch;

        ArmadaJobWatcher(@NonNull ArmadaCloud cloud) {
            this.cloudName = cloud.name;
            jobManagerValidity = ArmadaState.getJobManager(cloud).getValidity();
        }

        void stop() {
            if (watch != null) {
                LOGGER.info("Stopping watch for armada cloud " + cloudName);
                try {
                    this.watch.close();
                } catch (IOException e) {
                }
            }
        }

        @Override
        public void onClose() {
            LOGGER.fine(() -> cloudName + " watcher closed");
            Reaper.this.watchers.remove(cloudName, this);
        }

        @Override
        public void onEvent(EventOuterClass.EventMessage message) {
            if(!ArmadaClientUtil.isInTerminalState(message.getEventsCase())) {
                return;
            }

            var jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                return;
            }

            var metadata = ArmadaClientUtil.extractMetadata(message);
            var optionalNode = resolveNode(jenkins, metadata.getJobId(), metadata.getJobSetId());
            if(!optionalNode.isPresent()) {
                return;
            }

            var jobSetId = metadata.getJobSetId();
            var jobId = metadata.getJobId();
            Listeners.notify(Listener.class, true, listener -> {
                try {
                    var terminationReasons = Reaper.this.terminationReasons.get(optionalNode.get().getNodeName());
                    listener.onEvent(optionalNode.get(), metadata, message, terminationReasons);
                } catch (Exception x) {
                    LOGGER.log(Level.WARNING, "Listener " + listener + " failed for " + jobSetId + "/" + jobId, x);
                }
            });
        }
    }

    /**
     * Get any reason(s) why a node was terminated by a listener.
     * @param node a {@link Node#getNodeName}
     * @return a possibly empty set of {@link ContainerStateTerminated#getReason} or {@link PodStatus#getReason}
     */
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

        void onEvent(
                @NonNull ArmadaSlave node,
                @NonNull ArmadaJobMetadata metadata,
                @NonNull EventOuterClass.EventMessage message,
                @NonNull Set<String> terminationReasons)
                throws IOException, InterruptedException;
    }

    @Extension
    public static class RemoveAgentOnPodCancelled implements Listener {
        @Override
        public void onEvent(
                @NonNull ArmadaSlave node,
                @NonNull ArmadaJobMetadata metadata,
                @NonNull EventOuterClass.EventMessage message,
                @NonNull Set<String> terminationReasons)
                throws IOException {
            if (!message.hasCancelled()) {
                return;
            }
            String jobSet = node.getArmadaJobSetId();
            String job = node.getArmadaJobId();
            LOGGER.info(() -> jobSet + "/" + job + " was just cancelled, so removing corresponding Jenkins agent");
            node.getRunListener().getLogger().printf("Job %s/%s was just cancelled%n", jobSet, job);
            Jenkins.get().removeNode(node);
            disconnectComputer(node, new PodOfflineCause(Messages._PodOfflineCause_PodDeleted()));
        }
    }

    @Extension
    public static class TerminateAgentOnJobFailed implements Listener {
        @Override
        public void onEvent(
                @NonNull ArmadaSlave node,
                @NonNull ArmadaJobMetadata metadata,
                @NonNull EventOuterClass.EventMessage message,
                @NonNull Set<String> terminationReasons)
                throws IOException, InterruptedException {
            if (!ArmadaClientUtil.isInFailedState(message.getEventsCase())) {
                return;
            }

            var reason = metadata.getReason();
            var cause = metadata.getCause();
            logAndCleanUp(
                    node,
                    terminationReasons,
                    reason,
                    cause,
                    new StringBuilder()
                            .append(metadata.getJobSetId())
                            .append("/")
                            .append(metadata.getJobId())
                            .append(" Job just failed."),
                    node.getRunListener(),
                    new PodOfflineCause(Messages._PodOfflineCause_PodFailed(reason, message)));
        }
    }

    private static void logAndCleanUp(
            ArmadaSlave node,
            Set<String> terminationReasons,
            String reason,
            EventOuterClass.Cause failCause,
            StringBuilder sb,
            TaskListener runListener,
            PodOfflineCause cause) {
        List<String> details = new ArrayList<>();
        if (reason != null) {
            details.add("Reason: " + reason);
            terminationReasons.add(reason);
        }
        if (!details.isEmpty()) {
            sb.append(" ").append(String.join(", ", details)).append(".");
        }
        if(failCause != null) {
            sb.append(" failure cause: ").append(failCause);
            terminationReasons.add(failCause.name());
        }
        LOGGER.info(() -> sb + " Removing corresponding node " + node.getNodeName() + " from Jenkins.");
        runListener.getLogger().println(sb);
        PodUtils.cancelQueueItemFor(node, "PodFailure");
        disconnectComputer(node, cause);
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
    public static class ReaperSaveableListener extends SaveableListener {
        @Override
        public void onChange(Saveable o, XmlFile file) {
            if (o instanceof Jenkins) {
                Reaper reaper = Reaper.getInstance();
                // only update if reaper has been activated to avoid hitting api server if not in use
                if (reaper.activated.get()) {
                    Reaper.getInstance().watchClouds();
                }
            }
        }
    }
}
