package io.armadaproject.jenkins.plugin.job;

import hudson.model.Saveable;
import io.armadaproject.ArmadaClient;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArmadaJobManager implements Serializable, ArmadaClientProvider  {
    private static final Logger LOGGER = Logger.getLogger(ArmadaJobManager.class.getName());

    private final Object jobSetIdLock = new Object();
    private final ConcurrentMap<String, ArmadaJobSetManager> jobSetManagers = new ConcurrentHashMap<>();
    private final Saveable save;

    private volatile String currentJobSetId;
    private volatile ArmadaClientParameters parameters;

    public ArmadaJobManager(ArmadaClientParameters parameters, Saveable save) {
        this.parameters = parameters;
        this.save = save;
    }

    public boolean reconfigure(ArmadaClientParameters parameters) {
        var current = this.parameters;
        var changed = false;

        if(!current.jobSetStrategy.equals(parameters.jobSetStrategy)) {
            changed = true;
        }

        // changed api url, close all jobset managers/kill all jobs
        if(!current.apiUrl.equals(parameters.apiUrl)) {
            jobSetManagers.forEach((k, jsm) -> jsm.close());
            jobSetManagers.clear();
            changed = true;
        } else if(current.apiPort != parameters.apiPort ||
                  !current.queue.equals(parameters.queue) ||
                  !current.namespace.equals(parameters.namespace)||
                  !StringUtils.equals(current.credentialsId, parameters.credentialsId)) {
            jobSetManagers.forEach((k, jsm) -> jsm.reconfigure(parameters.namespace, parameters.queue));
            changed = true;
        }

        if(changed) {
            this.parameters = parameters;
        }

        return changed;
    }

    public ArmadaJobMetadata ensurePod(String existingJobSetId, String existingJobId, Pod pod) {
        ArmadaJobMetadata result;
        if(existingJobSetId != null && existingJobId != null) {
            result = getJobSetManager(existingJobSetId).ensureJob(pod, existingJobId);
        } else {
            result = getJobSetManager(computeJobSetId()).ensureJob(pod, null);
        }

        trySave();
        return result;
    }

    public void cancelJob(String jobSetId, String jobId) {
        getJobSetManager(jobSetId).cancelJob(jobId);
        trySave();
    }

    public boolean hasFailed(String jobSetId, String jobId) {
        return getJobSetManager(jobSetId).hasFailed(jobId);
    }

    public boolean hasTerminated(String jobSetId, String jobId) {
        return getJobSetManager(jobSetId).hasTerminated(jobId);
    }

    public ArmadaJobMetadata waitUntilRunning(String jobSetId, String jobId, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        return getJobSetManager(jobSetId).waitUntilRunning(jobId, timeout, unit);
    }

    public void close() {
        jobSetManagers.forEach((k, jsm) -> jsm.close());
        jobSetManagers.clear();
    }

    @Override
    public ArmadaClient get() throws KubernetesAuthException {
        return ArmadaState.createClient(parameters);
    }

    public void cleanupAbandonedJobSets() {
        var jobSetIds = new HashSet<>(jobSetManagers.keySet());
        for(var jobSetId : jobSetIds) {
            var jobSetManager = getJobSetManager(jobSetId);
            if(jobSetManager.isAbandoned() && !jobSetManager.hasActiveJobs()) {
                var removed = jobSetManagers.remove(jobSetId);
                if(removed != null) {
                    removed.close();
                }
            }
        }
    }

    private synchronized void trySave() {
        try {
            save.save();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to save ArmadaJobManager state", e);
        }
    }

    private String computeJobSetId() {
        var currentJobSetId = parameters.jobSetStrategy.getCurrentJobSet();
        synchronized (jobSetIdLock) {
            if(!currentJobSetId.equals(this.currentJobSetId)) {
                this.currentJobSetId = currentJobSetId;
                abandonExpiredJobSetManagers();
            }
        }
        return currentJobSetId;
    }

    private void abandonExpiredJobSetManagers() {
        var toAbandon = new HashSet<>(jobSetManagers.keySet());
        if(currentJobSetId != null) {
            toAbandon.remove(currentJobSetId);
        }
        for(var jobSetId : toAbandon) {
            getJobSetManager(jobSetId).abandon();
        }
    }

    // this will actually initialize the job set manager and start watching for events
    private ArmadaJobSetManager getJobSetManager(String jobSet) {
        var params = parameters;
        var newJobSetManager = new ArmadaJobSetManager(params.queue, jobSet, params.namespace);
        var result = jobSetManagers.putIfAbsent(jobSet, newJobSetManager);
        if(result == null) {
            result = newJobSetManager;
            trySave();
        }
        result.initialize(this);
        return result;
    }
}
