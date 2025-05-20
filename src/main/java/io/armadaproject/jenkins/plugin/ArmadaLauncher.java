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

package io.armadaproject.jenkins.plugin;

import static io.armadaproject.jenkins.plugin.job.ArmadaClientUtil.lookoutUrlForJob;
import static java.util.logging.Level.INFO;

import api.SubmitOuterClass.JobState;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Functions;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import io.armadaproject.jenkins.plugin.job.ArmadaLaunchFailedOfflineCause;
import io.armadaproject.jenkins.plugin.job.ArmadaState;
import io.armadaproject.jenkins.plugin.pod.decorator.PodDecoratorException;
import io.armadaproject.jenkins.plugin.pod.retention.Reaper;
import io.fabric8.kubernetes.api.model.Pod;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.StatusRuntimeException;
import jenkins.metrics.api.Metrics;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Launches on Kubernetes the specified {@link ArmadaComputer} instance.
 */
public class ArmadaLauncher extends JNLPLauncher {
    // Report progress every 30 seconds
    private static final long REPORT_INTERVAL = TimeUnit.SECONDS.toMillis(30L);

    private static final Collection<JobState> JOB_TERMINATED_STATES =
            Collections.unmodifiableCollection(Arrays.asList(JobState.FAILED, JobState.SUCCEEDED,
                JobState.REJECTED));

    private static final Logger LOGGER = Logger.getLogger(ArmadaLauncher.class.getName());
    private static final int MAX_RETRIES = 5;

    private volatile boolean launched = false;

    /**
     * Provisioning exception if any.
     */
    @CheckForNull
    private transient Throwable problem;

    @DataBoundConstructor
    public ArmadaLauncher(String tunnel, String vmargs) {
        super(tunnel, vmargs);
    }

    public ArmadaLauncher() {
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
        if (!(computer instanceof ArmadaComputer)) {
            throw new IllegalArgumentException("This Launcher can be used only with KubernetesComputer");
        }
        // Activate reaper if it never got activated.
        Reaper.getInstance().maybeActivate();
        ArmadaComputer kubernetesComputer = (ArmadaComputer) computer;
        computer.setAcceptingTasks(false);
        ArmadaSlave agent = kubernetesComputer.getNode();
        if (agent == null) {
            throw new IllegalStateException("Node has been removed, cannot launch " + computer.getName());
        }
        if (launched) {
            LOGGER.log(INFO, "Agent has already been launched, activating: {0}", agent.getNodeName());
            computer.setAcceptingTasks(true);
            return;
        }

        var jobManager = ArmadaState.getJobManager(agent.getArmadaCloud());
        try {
            var retryCount = kubernetesComputer.getRetryCount();
            if(retryCount >= MAX_RETRIES) {
                computer.setTemporarilyOffline(true, new ArmadaLaunchFailedOfflineCause("Provisioning failed"));
                var associatedItem = agent.getItem();
                if(associatedItem != null) {
                    Jenkins.get().getQueue().cancel(associatedItem);
                }
                throw new RuntimeException("Agent failed to launch after " + MAX_RETRIES + " retries");
            }

            PodTemplate template = agent.getTemplate();
            ArmadaCloud cloud = agent.getArmadaCloud();
            Pod pod;
            try {
                pod = template.build(agent);
            } catch (PodDecoratorException e) {
                Run<?, ?> run = template.getRun();
                if (run != null) {
                    template.getListener().getLogger().println("Failed to build pod definition : " + e.getMessage());
                    PodUtils.cancelQueueItemFor(run.getUrl(), template.getLabel(), e.getMessage(), null);
                }
                e.printStackTrace(listener.fatalError("Failed to build pod definition"));
                setProblem(e);
                terminateOrLog(agent);
                return;
            }

            agent.setNamespace(cloud.getArmadaNamespace());
            agent.setPodSpec(pod.getSpec());
            var existingJobId = agent.getArmadaJobId();
            var jobMetadata = jobManager.ensurePod(agent.getArmadaJobSetId(), existingJobId, pod);
            agent.setArmadaJobId(jobMetadata.getJobId());
            agent.setArmadaJobSetId(jobMetadata.getJobSetId());
            agent.save();

            String armadaLookoutJobUrl = lookoutUrlForJob(
                    cloud.getArmadaLookoutUrl(),
                    Integer.parseInt(cloud.getArmadaLookoutPort()),
                    cloud.getArmadaQueue(),
                    agent.getArmadaJobSetId(),
                    agent.getArmadaJobId());
            LOGGER.log(INFO, () -> "Submitted job: " + armadaLookoutJobUrl);
            listener.getLogger().printf("Submitted job: %s %n", armadaLookoutJobUrl);
            Metrics.metricRegistry().counter(MetricNames.JOBS_SUBMITTED).inc();

            agent.getRunListener().getLogger().printf("Submitted job: %s %n",
                    armadaLookoutJobUrl);

            kubernetesComputer.setLaunching(true);

            var metadata = jobManager.waitUntilRunning(jobMetadata.getJobSetId(), jobMetadata.getJobId(), template.getSlaveConnectTimeout(), TimeUnit.SECONDS);
            LOGGER.log(INFO, () -> "Job is running: " + agent.getArmadaJobId());
            agent.getRunListener().getLogger().printf("Job is running: %s %n", agent.getArmadaJobId());
            agent.setClusterId(metadata.getClusterId());
            agent.assignPod(metadata.getPodName());
            agent.save();

            kubernetesComputer.clearRetryCount();

            // We need the pod to be running and connected before returning
            // otherwise this method keeps being called multiple times
            // so wait for agent to be online
            int waitForSlaveToConnect = template.getSlaveConnectTimeout();
            int waitedForSlave;

            SlaveComputer slaveComputer = null;
            String status = null;
            long lastReportTimestamp = System.currentTimeMillis();
            for (waitedForSlave = 0; waitedForSlave < waitForSlaveToConnect; waitedForSlave++) {
                slaveComputer = agent.getComputer();
                if (slaveComputer == null) {
                    Metrics.metricRegistry().counter(MetricNames.LAUNCH_FAILED).inc();
                    throw new IllegalStateException("Node was deleted, computer is null");
                }
                if (slaveComputer.isOnline()) {
                    break;
                }

                // Check that the job hasn't failed already
                if (jobManager.hasFailed(jobMetadata.getJobSetId(), jobMetadata.getJobId())) {
                    Metrics.metricRegistry().counter(MetricNames.LAUNCH_FAILED).inc();
                    throw new IllegalStateException("Job failed: "
                        + agent.getArmadaJobId());
                }

                if (jobManager.hasTerminated(jobMetadata.getJobSetId(), jobMetadata.getJobId())) {
                    Metrics.metricRegistry().counter(MetricNames.LAUNCH_FAILED).inc();
                    Metrics.metricRegistry()
                            .counter(MetricNames.metricNameForPodStatus("TERMINATED"))
                            .inc();
                    throw new IllegalStateException("Job '" + agent.getArmadaJobId()
                        + "' is in terminated state");
                }

                if (lastReportTimestamp + REPORT_INTERVAL < System.currentTimeMillis()) {
                    LOGGER.log(INFO, "Waiting for agent to connect ({1}/{2}): {0}", new Object[] {
                            agent.getArmadaJobId(), waitedForSlave, waitForSlaveToConnect
                    });
                    listener.getLogger()
                            .printf(
                                    "Waiting for agent to connect (%2$s/%3$s): %1$s%n",
                                    agent.getArmadaJobId(), waitedForSlave, waitForSlaveToConnect);
                    lastReportTimestamp = System.currentTimeMillis();
                }
                Thread.sleep(1000);
            }
            if (slaveComputer == null || slaveComputer.isOffline()) {
                Metrics.metricRegistry().counter(MetricNames.LAUNCH_FAILED).inc();
                Metrics.metricRegistry().counter(MetricNames.FAILED_TIMEOUT).inc();

                throw new IllegalStateException(
                        "Agent is not connected after " + waitedForSlave + " seconds, status: " + status);
            }

            computer.setAcceptingTasks(true);
            launched = true;
            try {
                // We need to persist the "launched" setting...
                agent.save();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not save() agent: " + e.getMessage(), e);
            }
            Metrics.metricRegistry().counter(MetricNames.JOBS_LAUNCHED).inc();
        } catch (Throwable ex) {
            setProblem(ex);
            Functions.printStackTrace(ex, agent.getRunListener().error("Failed to launch " + agent.getArmadaJobId()));
            LOGGER.log(
                    Level.WARNING,
                    String.format("Error in provisioning; agent=%s, template=%s", agent, agent.getTemplateId()),
                    ex);
            LOGGER.log(Level.FINER, "Removing Jenkins node: {0}", agent.getNodeName());
            if(ex instanceof StatusRuntimeException) {
                Metrics.metricRegistry().counter(MetricNames.CREATION_FAILED).inc();
            }
            kubernetesComputer.incrementRetry();
            terminateOrLog(agent);
            throw new RuntimeException(ex);
        }
    }

    private static void terminateOrLog(ArmadaSlave node) {
        try {
            node.terminate();
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Unable to remove Jenkins node", e);
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
