package io.armadaproject.jenkins.plugin.job;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.model.listeners.SaveableListener;
import io.armadaproject.ArmadaClient;
import io.armadaproject.jenkins.plugin.ArmadaCloud;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.armadaproject.jenkins.plugin.KubernetesFactoryAdapter.resolveCredentials;

public class ArmadaState implements Saveable, Serializable {
    private static final Logger LOGGER = Logger.getLogger(ArmadaState.class.getName());

    private static ArmadaState instance;
    private final ConcurrentMap<String, ArmadaJobManager> jobManagers = new ConcurrentHashMap<>();

    @Extension
    public static final class PeriodicSave extends AsyncPeriodicWork {
        public PeriodicSave() {
            super("Periodic save of armada plugin state");
        }

        @Override
        protected void execute(TaskListener listener) throws IOException, InterruptedException {
            ArmadaState.getInstance().save();
        }

        @Override
        public long getRecurrencePeriod() {
            return TimeUnit.SECONDS.toMillis(60);
        }
    }

    @Extension(ordinal = 1)
    public static class SaveableListenerImpl extends SaveableListener {
        @Override
        public void onChange(Saveable o, XmlFile file) {
            if (o instanceof Jenkins) {
                Jenkins jenkins = (Jenkins) o;
                getInstance().reconfigure(jenkins.clouds.getAll(ArmadaCloud.class));
            }
            super.onChange(o, file);
        }
    }

    protected ArmadaState() {
        LOGGER.info("ArmadaState created");
    }

    public static ArmadaJobManager getJobManager(ArmadaCloud cloud) {
        return getInstance().doGetJobManager(cloud);
    }

    private void reconfigure(List<ArmadaCloud> clouds) {
        var keys = new HashSet<>(jobManagers.keySet());
        var changed = false;
        for (var cloud : clouds) {
            String displayName = cloud.getDisplayName();
            var jobManager = getJobManager(cloud);
            changed = jobManager.reconfigure(toParameters(cloud));
            keys.remove(displayName);
        }
        for(var cloudName : keys) {
            changed = true;
            jobManagers.remove(cloudName).close();
        }

        if(changed) {
            trySave();
        }
    }

    private static ArmadaClientParameters toParameters(ArmadaCloud cloud) {
        return new ArmadaClientParameters(
                cloud.getArmadaUrl(),
                Integer.parseInt(cloud.getArmadaPort()),
                cloud.getArmadaQueue(),
                cloud.getArmadaNamespace(),
                cloud.getArmadaCredentialsId(),
                cloud.getJobSetStrategy()
        );
    }

    private ArmadaJobManager doGetJobManager(ArmadaCloud cloud) {
        jobManagers.computeIfAbsent(cloud.getDisplayName(), (cloudName) -> new ArmadaJobManager(toParameters(cloud), this));

        trySave();
        return jobManagers.get(cloud.getDisplayName());
    }

    public static ArmadaClient createClient(ArmadaClientParameters params) throws KubernetesAuthException {
        if(params.credentialsId == null || params.credentialsId.isEmpty()) {
            return new ArmadaClient(params.apiUrl, params.apiPort);
        }

        StandardCredentials standardCredentials = resolveCredentials(params.credentialsId);
        if (!(standardCredentials instanceof StringCredentials)) {
            throw new KubernetesAuthException("credentials not a string credentials");
        }

        String secret = ((StringCredentials) standardCredentials).getSecret().getPlainText();

        return new ArmadaClient(params.apiUrl, params.apiPort, secret);
    }

    @Override
    public void save() throws IOException {
        getConfigFile().write(this);
    }

    protected void runCleanup() {
        jobManagers.forEach((cloudName, jobManager) -> {
            jobManager.cleanupAbandonedJobSets();
        });
    }

    protected static synchronized ArmadaState getInstance() {
        if (instance == null) {
            var configFile = getConfigFile();
            if (!configFile.exists()) {
                instance = createNewAndSave();
            } else {

                try {
                    instance = (ArmadaState) configFile.read();
                } catch (Throwable e) {
                    try {
                        configFile.delete();
                    } catch (Throwable ex) {
                        LOGGER.log(Level.SEVERE, "Failed to delete config file", ex);
                        throw new RuntimeException(e);
                    }
                    instance = createNewAndSave();
                }
            }
        }
        return instance;
    }

    private static ArmadaState createNewAndSave() {
        ArmadaState result;
        result = new ArmadaState();
        try {
            result.save();
        } catch(Throwable ex) {
            LOGGER.log(Level.SEVERE, "Failed to save empty config file", ex);
        }
        return result;
    }

    private void trySave() {
        try {
            save();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save armada state", e);
        }
    }

    private static XmlFile getConfigFile() {
        var dir = new File(Jenkins.get().getRootDir(), "armada-plugin");
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.log(Level.WARNING, "Failed to create directory: {0}", dir.getAbsolutePath());
        }
        return new XmlFile(Jenkins.XSTREAM, new File(dir, "job-manager.xml"));
    }
}
