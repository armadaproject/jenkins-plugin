package io.armadaproject.jenkins.plugin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Manages the Kubernetes client creation per cloud
 */
public class KubernetesClientProvider {

    private static final Logger LOGGER = Logger.getLogger(KubernetesClientProvider.class.getName());

    /**
     * Client expiration in seconds.
     *
     * Some providers such as Amazon EKS use a token with 15 minutes expiration, so expire clients after 10 minutes.
     */
    private static final long CACHE_EXPIRATION = Long.getLong(
            KubernetesClientProvider.class.getPackage().getName() + ".clients.cacheExpiration",
            TimeUnit.MINUTES.toSeconds(10));

    private static final Cache<CacheKey, Client> clients = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_EXPIRATION, TimeUnit.SECONDS)
            .removalListener((key, value, cause) -> {
                Client client = (Client) value;
                if (client != null) {
                    LOGGER.log(
                            Level.FINE, () -> "Expiring Kubernetes client " + key + " " + client.client + ": " + cause);
                }
            })
            .build();

    private KubernetesClientProvider() {}

    static KubernetesClient createClient(ArmadaCloud cloud, String serverUrl, String caCertData) {
        CacheKey cacheKey = new CacheKey(cloud.getDisplayName(), serverUrl, caCertData);
        Client cachedClient = clients.getIfPresent(cacheKey);

        if (cachedClient != null) {
            return cachedClient.getClient();
        }

        KubernetesClient newClient = createClient(cloud, cloud.getArmadaNamespace(), caCertData);
        clients.put(cacheKey, new Client(getValidity(cloud, serverUrl, caCertData), newClient));
        LOGGER.log(Level.FINE, "Created new Kubernetes client: {0} {1}", new Object[] {cacheKey, newClient});
        return newClient;
    }

    /**
     * Compute the hash of connection properties of the given cloud. This hash can be used to determine if a cloud
     * was updated and a new connection is needed.
     * @param cloud cloud to compute validity hash for
     * @return client validity hash code
     */
    @Restricted(NoExternalUse.class)
    public static int getValidity(@NonNull ArmadaCloud cloud, String serverUrl, String caCertData) {
        Object[] cloudObjects = {
                serverUrl,
            cloud.getArmadaNamespace(),
                caCertData,
            cloud.getCredentialsId(),
            cloud.getConnectTimeout(),
            cloud.getReadTimeout(),
            cloud.getMaxRequestsPerHostStr(),
            cloud.isUseJenkinsProxy()
        };
        return Arrays.hashCode(cloudObjects);
    }

    private static class Client {
        private final KubernetesClient client;
        private final int validity;

        public Client(int validity, KubernetesClient client) {
            this.client = client;
            this.validity = validity;
        }

        public KubernetesClient getClient() {
            return client;
        }

        public int getValidity() {
            return validity;
        }
    }

    public static class CacheKey {
        private final String cloudDisplayName;
        private final String serverUrl;
        private final String caCertData;

        public CacheKey(String cloudDisplayName, String serverUrl, String caCertData) {
            this.cloudDisplayName = cloudDisplayName;
            this.serverUrl = serverUrl;
            this.caCertData = caCertData;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CacheKey)) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(cloudDisplayName, cacheKey.cloudDisplayName) && Objects.equals(serverUrl, cacheKey.serverUrl) && Objects.equals(caCertData, cacheKey.caCertData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cloudDisplayName, serverUrl, caCertData);
        }
    }

    @Restricted(NoExternalUse.class) // testing only
    public static void invalidate(CacheKey cacheKey) {
        clients.invalidate(cacheKey);
    }

    @Restricted(NoExternalUse.class) // testing only
    public static void invalidateAll() {
        clients.invalidateAll();
    }

    // set ordinal to 1 so it runs ahead of Reaper
    @Extension(ordinal = 1)
    public static class SaveableListenerImpl extends SaveableListener {
        @Override
        public void onChange(Saveable o, XmlFile file) {
            if (o instanceof Jenkins) {
                Jenkins jenkins = (Jenkins) o;
                Set<CacheKey> cacheKeys = new HashSet<>(clients.asMap().keySet());
                for (ArmadaCloud cloud : jenkins.clouds.getAll(ArmadaCloud.class)) {
                    String displayName = cloud.getDisplayName();
                    Set<CacheKey> cloudCacheKeys = cacheKeys.stream()
                            .filter(c -> displayName.equals(c.cloudDisplayName))
                            .collect(Collectors.toSet());

                    for(CacheKey cacheKey : cloudCacheKeys) {
                        Client client = clients.getIfPresent(cacheKey);
                        if (client == null || client.getValidity() == getValidity(cloud, cacheKey.serverUrl, cacheKey.caCertData)) {
                            cacheKeys.remove(cacheKey);
                        }
                    }
                }
                // Remove missing / invalid clients
                for (CacheKey cacheKey : cacheKeys) {
                    LOGGER.log(
                            Level.INFO,
                            () -> "Invalidating Kubernetes client: " + cacheKey.cloudDisplayName + "-" + cacheKey.serverUrl + clients.getIfPresent(cacheKey));
                    invalidate(cacheKey);
                }
            }
            super.onChange(o, file);
        }
    }
}
