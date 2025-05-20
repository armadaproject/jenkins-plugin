package io.armadaproject.jenkins.plugin;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.Task;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;

@Extension
@SuppressWarnings({"rawtypes"})
public class ArmadaQueueTaskDispatcher extends QueueTaskDispatcher {

    @Override
    public CauseOfBlockage canTake(Node node, Queue.BuildableItem item) {
        if (node instanceof ArmadaSlave) {
            ArmadaSlave slave = (ArmadaSlave) node;
            Task ownerTask = item.task.getOwnerTask();
            slave.assignTask(item);
            if (!ArmadaFolderProperty.isAllowed(slave, (Job) ownerTask)) {
                return new KubernetesCloudNotAllowed(slave.getArmadaCloud(), (Job) ownerTask);
            }
        }
        return null;
    }

    public static final class KubernetesCloudNotAllowed extends CauseOfBlockage {

        private final ArmadaCloud cloud;
        private final Job job;

        public KubernetesCloudNotAllowed(ArmadaCloud cloud, Job job) {
            this.cloud = cloud;
            this.job = job;
        }

        @Override
        public String getShortDescription() {
            return Messages.KubernetesCloudNotAllowed_Description(cloud.name, job.getFullName());
        }
    }
}
