package io.armadaproject.jenkins.plugin.job;

import api.EventOuterClass;
import api.Job;
import api.SubmitOuterClass;
import io.armadaproject.ArmadaMapper;
import io.fabric8.kubernetes.api.model.Pod;
import jenkins.metrics.api.Metrics;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;

import java.io.Serializable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.armadaproject.jenkins.plugin.MetricNames.metricNameForPodStatus;
import static io.armadaproject.jenkins.plugin.job.ArmadaClientUtil.*;

public class ArmadaJobSetManager implements Serializable, ArmadaJobSetEventWatcher.ArmadaMessageCallback {
    private static final Logger LOGGER = Logger.getLogger(ArmadaJobSetManager.class.getName());

    private final ConcurrentMap<String, JobStatus> knownJobs = new ConcurrentHashMap<>();
    private final String jobSetId;
    private volatile String queue;
    private volatile String namespace;
    private volatile String lastMessageId = null;
    private volatile boolean abandoned = false;

    private transient volatile ArmadaJobSetEventWatcher watcher;
    private transient volatile Thread watcherThread;
    private transient volatile ArmadaClientProvider clientProvider;
    private transient volatile ArmadaJobNotifier jobNotifier;

    private static class JobStatus {
        private final ArmadaJobMetadata metadata;
        private final SubmitOuterClass.JobState state;

        private JobStatus(ArmadaJobMetadata metadata, SubmitOuterClass.JobState state) {
            this.metadata = metadata;
            this.state = state;
        }
    }

    public ArmadaJobSetManager(String queue, String jobSetId, String namespace) {
        if(StringUtils.isEmpty(queue)) {
            throw new IllegalArgumentException("queue is empty");
        }

        if(StringUtils.isEmpty(jobSetId)) {
            throw new IllegalArgumentException("jobSetId is empty");
        }

        if(StringUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("namespace is empty");
        }

        this.queue = queue;
        this.jobSetId = jobSetId;
        this.namespace = namespace;
    }

    public void abandon() {
        this.abandoned = true;
    }

    public boolean isAbandoned() {
        return this.abandoned;
    }

    public boolean hasActiveJobs() {
        if(knownJobs.isEmpty()) {
            return false;
        }

        if(knownJobs.values().stream().noneMatch(s -> s.state != SubmitOuterClass.JobState.UNKNOWN && !isInTerminalState(s.state))) {
            return false;
        }

        try(var client = clientProvider.get()) {
            var response = client.getJobStatus(Job.JobStatusRequest.newBuilder().addAllJobIds(knownJobs.keySet()).build());
            return response.getJobStatesMap().values().stream().anyMatch(s -> s != SubmitOuterClass.JobState.UNKNOWN && !isInTerminalState(s));
        } catch (KubernetesAuthException e) {
            LOGGER.log(Level.SEVERE, "Error while querying known job statuses jobset " + jobSetId, e);
            //return false here, this will make sure everything is closed down if run into an error
            return false;
        }
    }

    public void cancelJob(String jobId) {
        try(var client = clientProvider.get()) {
            var response = client.getJobStatus(Job.JobStatusRequest.newBuilder().addJobIds(jobId).build());
            if (!isInTerminalState(response.getJobStatesMap().get(jobId))) {
                client.cancelJob(SubmitOuterClass.JobCancelRequest.newBuilder()
                        .setQueue(queue)
                        .setJobSetId(jobSetId)
                        .setJobId(jobId)
                        .build());

                String msg = ("Cancelled job id: " + jobId + " with job set id: "
                        + jobSetId);
                LOGGER.info(msg);
            } else {
                String msg = ("No jobs in running state for id: " + jobId + " with job set id: "
                        + jobSetId);
                LOGGER.log(Level.WARNING, msg);
            }
        } catch (KubernetesAuthException e) {
            LOGGER.log(Level.SEVERE, "Error while cancelling job", e);
            throw new RuntimeException(e);
        }
    }

    public ArmadaJobMetadata ensureJob(Pod pod, String existingJobId) {
        if(abandoned) {
            throw new IllegalStateException("JobSet abandoned");
        }

        try(var client = clientProvider.get()) {
            tryRefreshJobState(existingJobId);
            // if the controller was interrupted after creating the pod but before it connected back, then
            // the pod might already exist and the creating logic must be skipped.
            var needsSubmit = existingJobId == null;
            if(existingJobId != null && knownJobs.containsKey(existingJobId)) {
                var knownJob = knownJobs.getOrDefault(existingJobId, null);
                needsSubmit = knownJob == null || isInTerminalState(knownJob.state);
                if(knownJob != null && isInTerminalState(knownJob.state)) {
                    knownJobs.remove(existingJobId);
                }
            } else {
                needsSubmit = true;
            }

            if(needsSubmit) {
                ArmadaMapper armadaMapper = new ArmadaMapper(queue, namespace, jobSetId, pod);
                var jobSubmitResponse = client.submitJob(armadaMapper.createJobSubmitRequest());
                var jobId =  jobSubmitResponse.getJobResponseItems(0).getJobId();
                knownJobs.put(jobId, new JobStatus(null, SubmitOuterClass.JobState.UNKNOWN));
                return new ArmadaJobMetadata(jobSetId, jobId, null, null);
            }

            return new ArmadaJobMetadata(jobSetId, existingJobId, null, null);
        } catch (KubernetesAuthException e) {
            LOGGER.log(Level.SEVERE, "Failed to create job", e);
            throw new RuntimeException(e);
        }
    }

    public void initialize(ArmadaClientProvider clientProvider) {
        if(watcher == null) {
            this.clientProvider = clientProvider;
            this.jobNotifier = new ArmadaJobNotifier();

            // initialize known job states if we are loading from saved state
            try(var client = clientProvider.get()) {
                var request = Job.JobStatusRequest.newBuilder().addAllJobIds(knownJobs.keySet()).build();
                var response = client.getJobStatus(request);
                var jobStateMap = response.getJobStatesMap();
                jobStateMap.forEach((jobId, jobState) -> {
                    if(isInTerminalState(jobState)) {
                        knownJobs.remove(jobId);
                    } else {
                        knownJobs.computeIfPresent(jobId, (k, v) -> new JobStatus(v.metadata, jobState));
                    }
                });
            } catch (KubernetesAuthException e) {
                LOGGER.log(Level.SEVERE, "Failed to set armada job client", e);
                throw new RuntimeException(e);
            }

            watcher = new ArmadaJobSetEventWatcher(clientProvider, this, queue, jobSetId, lastMessageId);
            watcherThread = new Thread(watcher);
            watcherThread.setDaemon(true);
            watcherThread.start();
        }
    }

    public ArmadaJobMetadata waitUntilRunning(String jobId, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        var job = knownJobs.getOrDefault(jobId, null);
        if(job == null) {
            throw new IllegalArgumentException("Unknown job: " + jobId);
        }

        // check if we already know the job is running
        if(job.state == SubmitOuterClass.JobState.RUNNING) {
            return job.metadata;
        }

        final AtomicReference<ArmadaJobMetadata> metadata = new AtomicReference<>(job.metadata);
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final AtomicReference<RuntimeException> error = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        ArmadaJobNotifier.Callback callback = new ArmadaJobNotifier.Callback() {
            @Override
            public void accept(ArmadaJobMetadata newMetadata) {
                metadata.set(newMetadata);
                latch.countDown();
            }

            @Override
            public void error(RuntimeException e) {
                error.set(e);
                latch.countDown();
            }

            @Override
            public void cancelled() {
                cancelled.set(true);
                latch.countDown();
            }
        };
        // subscribe to get running events
        jobNotifier.subscribe(jobId, callback);

        var timedOut = false;
        // check again if it maybe already running before waiting
        // if the job terminated it has already been removed
        job = knownJobs.getOrDefault(jobId, null);
        if(job == null) {
            jobNotifier.unsubscribe(jobId, callback);
            throw new IllegalArgumentException("Job already terminated: " + jobId);
        } else if(job.state != SubmitOuterClass.JobState.RUNNING) {
            timedOut = !latch.await(timeout, unit);
        } else {
            jobNotifier.unsubscribe(jobId, callback);
            return job.metadata;
        }

        if(!cancelled.get()) {
            jobNotifier.unsubscribe(jobId, callback);
        } else {
            throw new CancellationException("Armada event watcher cancelled");
        }

        var err = error.get();
        if(err != null) {
            throw err;
        }

        if(timedOut) {
            job = knownJobs.get(jobId);
            if(job.state == SubmitOuterClass.JobState.RUNNING) {
                return job.metadata;
            } else {
                throw new TimeoutException("Timed out waiting for job: " + jobId);
            }
        } else {
            return metadata.get();
        }
    }

    public boolean hasFailed(String jobId) {
        var job = knownJobs.getOrDefault(jobId, null);
        return job != null && isInFailedState(job.state);
    }

    public boolean hasTerminated(String jobId) {
        var job = knownJobs.getOrDefault(jobId, null);
        return job != null && isInTerminalState(job.state);
    }

    @Override
    public void onMessage(EventOuterClass.EventStreamMessage eventStreamMessage) {
        var message = eventStreamMessage.getMessage();
        var eventsCase = message.getEventsCase();
        var jobMetadata = extractMetadata(message);

        var jobId = jobMetadata.getJobId();
        final var state = toJobState(eventsCase);

        Metrics.metricRegistry().counter(metricNameForPodStatus(state.toString())).inc();

        knownJobs.compute(jobId, (k, v) -> updateJobStatus(k, v, jobMetadata, state));
        if(state == SubmitOuterClass.JobState.RUNNING) {
            jobNotifier.notify(jobMetadata, null);
        } else if(isInFailedState(state)) {
            jobNotifier.notify(new ArmadaJobMetadata(jobSetId, jobId, null, null), new RuntimeException("Job " + state));
        }

        if(isInTerminalState(eventsCase)) {
            knownJobs.remove(jobId);
        }

        lastMessageId = eventStreamMessage.getId();
    }

    public void reconfigure(String namespace, String queue) {
        this.namespace = namespace;
        this.queue = queue;
        var currentWatcher = watcher;
        if(currentWatcher != null) {
            currentWatcher.forceReconnect(queue);
        }
    }

    public void close() {
        var currentWatcher = watcher;
        watcher = null;
        if(currentWatcher != null) {
            currentWatcher.close();
            try {
                watcherThread.join();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Interrupted while waiting for watcher to complete", e);
            }
        }

        jobNotifier.close();

        try(var client = clientProvider.get()) {
            client.cancelJob(SubmitOuterClass.JobCancelRequest.newBuilder()
                    .setQueue(queue)
                    .setJobSetId(jobSetId)
                    .addAllJobIds(knownJobs.keySet())
                    .build());
        } catch (KubernetesAuthException e) {
            LOGGER.log(Level.SEVERE, "Error while cancelling jobs", e);
        }
    }

    private static JobStatus updateJobStatus(String k, JobStatus v, ArmadaJobMetadata jobMetadata, SubmitOuterClass.JobState state) {
        return new JobStatus(v != null && v.metadata != null ? v.metadata.mergeWith(jobMetadata) : jobMetadata,
                state != SubmitOuterClass.JobState.UNKNOWN ? state : (v == null ? SubmitOuterClass.JobState.UNKNOWN : v.state));
    }

    private void tryRefreshJobState(String jobId)
    {
        if(jobId != null) {
            var request = Job.JobStatusRequest.newBuilder().addJobIds(jobId).build();
            try (var client = clientProvider.get()) {
                var response = client.getJobStatus(request);
                var jobStateMap = response.getJobStatesMap();
                if (jobStateMap.containsKey(jobId)) {
                    knownJobs.compute(jobId, (k, v) -> updateJobStatus(k, v, null, jobStateMap.get(jobId)));
                }
            } catch (KubernetesAuthException e) {
                LOGGER.log(Level.SEVERE, "Error while cancelling job", e);
            }
        }
    }
}
