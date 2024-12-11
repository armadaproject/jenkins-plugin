/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
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

package org.csanchez.jenkins.plugins.kubernetes;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import api.Job.JobStatusRequest;
import api.Job.JobStatusResponse;
import api.SubmitOuterClass.JobState;
import api.SubmitOuterClass.JobSubmitResponse;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Functions;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import io.armadaproject.ArmadaClient;
import io.armadaproject.ArmadaMapper;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jenkins.metrics.api.Metrics;
import org.apache.commons.lang.StringUtils;
import org.awaitility.Awaitility;
import org.csanchez.jenkins.plugins.kubernetes.pod.decorator.PodDecoratorException;
import org.csanchez.jenkins.plugins.kubernetes.pod.retention.Reaper;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Launches on Kubernetes the specified {@link KubernetesComputer} instance.
 */
public class KubernetesLauncher extends JNLPLauncher {
    // Report progress every 30 seconds
    private static final long REPORT_INTERVAL = TimeUnit.SECONDS.toMillis(30L);

    private static final Collection<JobState> JOB_TERMINATED_STATES =
            Collections.unmodifiableCollection(Arrays.asList(JobState.FAILED, JobState.SUCCEEDED,
                JobState.REJECTED));

    private static final Logger LOGGER = Logger.getLogger(KubernetesLauncher.class.getName());

    private volatile boolean launched = false;

    /**
     * Provisioning exception if any.
     */
    @CheckForNull
    private transient Throwable problem;

    @DataBoundConstructor
    public KubernetesLauncher(String tunnel, String vmargs) {
        super(tunnel, vmargs);
    }

    public KubernetesLauncher() {
        super();
    }

    @Override
    public boolean isLaunchSupported() {
        return !launched;
    }

    @Override
    @SuppressFBWarnings(value = {"SWL_SLEEP_WITH_LOCK_HELD", "REC_CATCH_EXCEPTION",
        "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification = "This is fine")
    public synchronized void launch(SlaveComputer computer, TaskListener listener) {
        if (!(computer instanceof KubernetesComputer)) {
            throw new IllegalArgumentException("This Launcher can be used only with KubernetesComputer");
        }
        // Activate reaper if it never got activated.
        Reaper.getInstance().maybeActivate();
        KubernetesComputer kubernetesComputer = (KubernetesComputer) computer;
        computer.setAcceptingTasks(false);
        KubernetesSlave node = kubernetesComputer.getNode();
        if (node == null) {
            throw new IllegalStateException("Node has been removed, cannot launch " + computer.getName());
        }
        if (launched) {
            LOGGER.log(INFO, "Agent has already been launched, activating: {0}", node.getNodeName());
            computer.setAcceptingTasks(true);
            return;
        }

        String cloudName = node.getCloudName();

        try {
            PodTemplate template = node.getTemplate();
            KubernetesCloud cloud = node.getKubernetesCloud();
            ArmadaClient armadaClient = cloud.connectToArmada();
            Pod pod;
            try {
                pod = template.build(node);
            } catch (PodDecoratorException e) {
                Run<?, ?> run = template.getRun();
                if (run != null) {
                    template.getListener().getLogger().println("Failed to build pod definition : " + e.getMessage());
                    PodUtils.cancelQueueItemFor(run.getUrl(), template.getLabel(), e.getMessage(), null);
                }
                e.printStackTrace(listener.fatalError("Failed to build pod definition"));
                setProblem(e);
                terminateOrLog(node);
                return;
            }
            node.assignPod(pod);

            String podName = pod.getMetadata().getName();

            String namespace = Arrays.asList( //
                            pod.getMetadata().getNamespace(), template.getNamespace()) //
                    .stream()
                    .filter(s -> StringUtils.isNotBlank(s))
                    .findFirst()
                    .orElse(null);
            node.setNamespace(namespace);

            // if the controller was interrupted after creating the pod but before it connected back, then
            // the pod might already exist and the creating logic must be skipped.
            JobStatusResponse jobStatusResponse = armadaClient.getJobStatus(
                JobStatusRequest.newBuilder()
                    .addJobIds(kubernetesComputer.getArmadaJobId())
                    .build());
            JobState existingJobState = jobStatusResponse.getJobStatesMap()
                .get(kubernetesComputer.getArmadaJobId());

            LOGGER.info("Job with id:" + kubernetesComputer.getArmadaJobId() + " in state: "
                + existingJobState);

            if (existingJobState == JobState.UNKNOWN) {
                LOGGER.log(FINE, () -> "Creating job: " + cloudName + "/" + podName);
                try {
                    ArmadaMapper armadaMapper = new ArmadaMapper(cloud.getArmadaQueue(),
                        cloud.getArmadaNamespace(), cloud.getArmadaQueue(), pod);

                    JobSubmitResponse jobSubmitResponse = armadaClient.submitJob(
                        armadaMapper.createJobSubmitRequest());
                    String jobId = jobSubmitResponse.getJobResponseItems(0).getJobId();
                    kubernetesComputer.setArmadaJobId(jobId);
                    ((KubernetesSlave) computer.getNode()).setArmadaJobId(jobId);
                } catch (KubernetesClientException e) {
                    Metrics.metricRegistry()
                            .counter(MetricNames.CREATION_FAILED)
                            .inc();
                    int httpCode = e.getCode();
                    if (400 <= httpCode && httpCode < 500) { // 4xx
                        if (httpCode == 403 && e.getMessage().contains("is forbidden: exceeded quota")) {
                            node.getRunListener()
                                    .getLogger()
                                    .printf(
                                            "WARNING: Unable to create pod: %s %s/%s because kubernetes resource quota exceeded. %n%s%nRetrying...%n%n",
                                            cloudName,
                                            namespace,
                                            pod.getMetadata().getName(),
                                            e.getMessage());
                        } else if (httpCode == 409
                                && e.getMessage().contains("Operation cannot be fulfilled on resourcequotas")) {
                            // See: https://github.com/kubernetes/kubernetes/issues/67761 ; A retry usually works.
                            node.getRunListener()
                                    .getLogger()
                                    .printf(
                                            "WARNING: Unable to create pod: %s %s/%s because kubernetes resource quota update conflict. %n%s%nRetrying...%n%n",
                                            cloudName,
                                            namespace,
                                            pod.getMetadata().getName(),
                                            e.getMessage());
                        } else {
                            node.getRunListener()
                                    .getLogger()
                                    .printf(
                                            "ERROR: Unable to create pod %s %s/%s.%n%s%n",
                                            cloudName,
                                            namespace,
                                            pod.getMetadata().getName(),
                                            e.getMessage());
                            PodUtils.cancelQueueItemFor(pod, e.getMessage());
                        }
                    } else if (500 <= httpCode && httpCode < 600) { // 5xx
                        LOGGER.log(FINE, "Kubernetes returned HTTP code {0} {1}. Retrying...", new Object[] {
                            e.getCode(), e.getStatus()
                        });
                    } else {
                        LOGGER.log(WARNING, "Kubernetes returned unhandled HTTP code {0} {1}", new Object[] {
                            e.getCode(), e.getStatus()
                        });
                    }
                    throw e;
                }
                LOGGER.log(INFO, () -> "Submitted job: " + kubernetesComputer.getArmadaJobId());
                listener.getLogger().printf("Submitted job: %s %n",
                    kubernetesComputer.getArmadaJobId());
                Metrics.metricRegistry().counter(MetricNames.PODS_CREATED).inc();

                node.getRunListener().getLogger().printf("Submitted job: %s %n",
                    kubernetesComputer.getArmadaJobId());
            } else {
                LOGGER.log(INFO, () -> "Job already exists: " +
                    kubernetesComputer.getArmadaJobId());
                listener.getLogger().printf("Job already exists: %s %n",
                    kubernetesComputer.getArmadaJobId());
            }
            kubernetesComputer.setLaunching(true);

            // not necessary
//            ObjectMeta podMetadata = pod.getMetadata();
//            template.getWorkspaceVolume().createVolume(client, podMetadata);
//            template.getVolumes().forEach(volume -> volume.createVolume(client, podMetadata));

            Awaitility.await().atMost(template.getSlaveConnectTimeout(),
                TimeUnit.SECONDS).until(() -> {
                JobStatusResponse jobStatus = armadaClient.getJobStatus(
                    JobStatusRequest.newBuilder()
                        .addJobIds(kubernetesComputer.getArmadaJobId())
                        .build());
                return jobStatus.getJobStatesMap().get(kubernetesComputer.getArmadaJobId())
                    == JobState.RUNNING;
            });
            LOGGER.log(INFO, () -> "Job is running: " + kubernetesComputer.getArmadaJobId());

            // We need the pod to be running and connected before returning
            // otherwise this method keeps being called multiple times
            // so wait for agent to be online
            int waitForSlaveToConnect = template.getSlaveConnectTimeout();
            int waitedForSlave;

            SlaveComputer slaveComputer = null;
            String status = null;
            List<ContainerStatus> containerStatuses = null;
            long lastReportTimestamp = System.currentTimeMillis();
            for (waitedForSlave = 0; waitedForSlave < waitForSlaveToConnect; waitedForSlave++) {
                slaveComputer = node.getComputer();
                if (slaveComputer == null) {
                    Metrics.metricRegistry().counter(MetricNames.LAUNCH_FAILED).inc();
                    throw new IllegalStateException("Node was deleted, computer is null");
                }
                if (slaveComputer.isOnline()) {
                    break;
                }

                // Check that the job hasn't failed already
                JobState jobState = armadaClient.getJobStatus(
                    JobStatusRequest.newBuilder()
                        .addJobIds(kubernetesComputer.getArmadaJobId())
                        .build()).getJobStatesMap().get(kubernetesComputer.getArmadaJobId());
                if (jobState == JobState.FAILED || jobState == JobState.REJECTED) {
                    Metrics.metricRegistry().counter(MetricNames.LAUNCH_FAILED).inc();
                    throw new IllegalStateException("Job failed: "
                        + kubernetesComputer.getArmadaJobId());
                }

                if (JOB_TERMINATED_STATES.contains(jobState)) {
                    Metrics.metricRegistry().counter(MetricNames.LAUNCH_FAILED).inc();
                    Metrics.metricRegistry()
                            .counter(MetricNames.metricNameForPodStatus(status))
                            .inc();
                    // TODO see how to do this
//                    logLastLines(containerStatuses, pod, node, null, client);
                    throw new IllegalStateException("Job '" + kubernetesComputer.getArmadaJobId()
                        + "' is in terminated state. State: " + jobState);
                }

                // TODO there are no longer container statutes
//                containerStatuses = pod.getStatus().getContainerStatuses();
//                List<ContainerStatus> terminatedContainers = new ArrayList<>();
//                for (ContainerStatus info : containerStatuses) {
//                    if (info != null) {
//                        if (info.getState().getTerminated() != null) {
//                            // Container has errored
//                            LOGGER.log(INFO, "Container is terminated {0} [{2}]: {1}", new Object[] {
//                                podName, info.getState().getTerminated(), info.getName()
//                            });
//                            listener.getLogger()
//                                    .printf(
//                                            "Container is terminated %1$s [%3$s]: %2$s%n",
//                                            podName, info.getState().getTerminated(), info.getName());
//                            Metrics.metricRegistry()
//                                    .counter(MetricNames.LAUNCH_FAILED)
//                                    .inc();
//                            terminatedContainers.add(info);
//                        }
//                    }
//                }

//                checkTerminatedContainers(terminatedContainers, pod, node, client);

                if (lastReportTimestamp + REPORT_INTERVAL < System.currentTimeMillis()) {
                    LOGGER.log(INFO, "Waiting for agent to connect ({1}/{2}): {0}", new Object[] {
                        podName, waitedForSlave, waitForSlaveToConnect
                    });
                    listener.getLogger()
                            .printf(
                                    "Waiting for agent to connect (%2$s/%3$s): %1$s%n",
                                    podName, waitedForSlave, waitForSlaveToConnect);
                    lastReportTimestamp = System.currentTimeMillis();
                }
                Thread.sleep(1000);
            }
            if (slaveComputer == null || slaveComputer.isOffline()) {
                Metrics.metricRegistry().counter(MetricNames.LAUNCH_FAILED).inc();
                Metrics.metricRegistry().counter(MetricNames.FAILED_TIMEOUT).inc();

//                logLastLines(containerStatuses, pod, node, null, client);
                throw new IllegalStateException(
                        "Agent is not connected after " + waitedForSlave + " seconds, status: " + status);
            }

            computer.setAcceptingTasks(true);
            launched = true;
            try {
                // We need to persist the "launched" setting...
                node.save();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not save() agent: " + e.getMessage(), e);
            }
            Metrics.metricRegistry().counter(MetricNames.PODS_LAUNCHED).inc();
        } catch (Throwable ex) {
            setProblem(ex);
            Functions.printStackTrace(ex, node.getRunListener().error("Failed to launch " + node.getPodName()));
            LOGGER.log(
                    Level.WARNING,
                    String.format("Error in provisioning; agent=%s, template=%s", node, node.getTemplateId()),
                    ex);
            LOGGER.log(Level.FINER, "Removing Jenkins node: {0}", node.getNodeName());
            terminateOrLog(node);
            throw new RuntimeException(ex);
        }
    }

    private static void terminateOrLog(KubernetesSlave node) {
        try {
            node.terminate();
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Unable to remove Jenkins node", e);
        }
    }

    private void checkTerminatedContainers(
            List<ContainerStatus> terminatedContainers,
            Pod pod,
            KubernetesSlave slave,
            KubernetesClient client) {
        if (!terminatedContainers.isEmpty()) {
            Map<String, Integer> errors = terminatedContainers.stream()
                    .collect(Collectors.toMap(
                            ContainerStatus::getName,
                            (info) -> info.getState().getTerminated().getExitCode()));

            // Print the last lines of failed containers
            logLastLines(terminatedContainers, pod, slave, errors, client);
            throw new IllegalStateException("Containers are terminated with exit codes: " + errors);
        }
    }

    /**
     * Log the last lines of containers logs
     */
    private void logLastLines(
            @CheckForNull List<ContainerStatus> containers,
            Pod pod,
            KubernetesSlave slave,
            Map<String, Integer> errors,
            KubernetesClient client) {
        if (containers != null) {
            for (ContainerStatus containerStatus : containers) {
                String containerName = containerStatus.getName();
                String log = client.pods()
                        .resource(pod)
                        .inContainer(containerStatus.getName())
                        .tailingLines(30)
                        .getLog();
                if (!StringUtils.isBlank(log)) {
                    String msg =
                            errors != null ? String.format(" exited with error %s", errors.get(containerName)) : "";
                    LOGGER.log(
                            Level.SEVERE,
                            "Error in provisioning; agent={0}, template={1}. Container {2}{3}. Logs: {4}",
                            new Object[] {slave, slave.getTemplateOrNull(), containerName, msg, log});
                }
            }
        }
    }

    /**
     * The last problem that occurred, if any.
     * @return
     */
    @CheckForNull
    public Throwable getProblem() {
        return problem;
    }

    public void setProblem(@CheckForNull Throwable problem) {
        this.problem = problem;
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        return new DescriptorImpl();
    }

    // Only there to avoid throwing unnecessary exceptions. KubernetesLauncher is never instantiated via UI.
    private static class DescriptorImpl extends Descriptor<ComputerLauncher> {}
}
