package io.armadaproject.jenkins.plugin;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.security.ACL;
import hudson.security.Permission;
import hudson.slaves.AbstractCloudComputer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;

/**
 * @author Carlos Sanchez carlos@apache.org
 */
public class ArmadaComputer extends AbstractCloudComputer<ArmadaSlave> {
    private static final Logger LOGGER = Logger.getLogger(ArmadaComputer.class.getName());
    private static final ConcurrentMap<String, AtomicInteger> retries = new ConcurrentHashMap<>();

    private boolean launching;

    public ArmadaComputer(ArmadaSlave slave) {
        super(slave);
    }

    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        super.taskAccepted(executor, task);
        Queue.Executable exec = executor.getCurrentExecutable();
        LOGGER.log(Level.FINE, " Computer {0} accepted task {1}", new Object[] {this, exec});
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        Queue.Executable exec = executor.getCurrentExecutable();
        LOGGER.log(Level.FINE, " Computer {0} completed task {1}", new Object[] {this, exec});

        // May take the agent offline and remove it, in which case getNode()
        // above would return null and we'd not find our DockerSlave anymore.
        super.taskCompleted(executor, task, durationMS);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
        Queue.Executable exec = executor.getCurrentExecutable();
        LOGGER.log(Level.FINE, " Computer {0} completed task {1} with problems", new Object[] {this, exec});
    }

    @Override
    public String toString() {
        return String.format("KubernetesComputer name: %s agent: %s", getName(), getNode());
    }

    @Override
    @NonNull
    public ACL getACL() {
        final ACL base = super.getACL();
        return new KubernetesComputerACL(base);
    }

    /**
     * Simple static inner class to be used by {@link #getACL()}.
     * It replaces an anonymous inner class in order to fix
     * <a href="https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#sic-could-be-refactored-into-a-named-static-inner-class-sic-inner-should-be-static-anon">SIC_INNER_SHOULD_BE_STATIC_ANON</a>.
     */
    private static final class KubernetesComputerACL extends ACL {

        private final ACL base;

        public KubernetesComputerACL(final ACL base) {
            this.base = base;
        }

        @Override
        public boolean hasPermission(Authentication a, Permission permission) {
            return permission == Computer.CONFIGURE ? false : base.hasPermission(a, permission);
        }
    }

    public void setLaunching(boolean launching) {
        this.launching = launching;
    }

    /**
     *
     * @return true if the Pod has been created in Kubernetes and the current instance is waiting for the pod to be usable.
     */
    public boolean isLaunching() {
        return launching;
    }

    @Override
    public void setAcceptingTasks(boolean acceptingTasks) {
        super.setAcceptingTasks(acceptingTasks);
        if (acceptingTasks) {
            launching = false;
        }
    }

    public int getRetryCount() {
        return retries.computeIfAbsent(getNode().getLabelString(), k -> new AtomicInteger(0)).get();
    }

    public void incrementRetry() {
        retries.computeIfAbsent(getNode().getLabelString(), k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void clearRetryCount() {
        retries.remove(getName());
    }
}
