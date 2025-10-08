package io.armadaproject.jenkins.plugin;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ArmadaClusterInfoProvider {
    @Extension(ordinal = 2)
    public static class SaveableListenerImpl extends SaveableListener {
        @Override
        public void onChange(Saveable o, XmlFile file) {
            if (o instanceof Jenkins) {
                Jenkins jenkins = (Jenkins) o;
                ArmadaClusterInfoProvider.reconfigure(jenkins.clouds.getAll(ArmadaCloud.class));
            }
            super.onChange(o, file);
        }
    }

    public static class ClusterInfo {
        private final String name;
        private final String apiUrl;
        private final String serverCertificate;

        public ClusterInfo(String name, String apiUrl, String serverCertificate) {
            this.name = name;
            this.apiUrl = apiUrl;
            this.serverCertificate = serverCertificate;
        }

        public String getName() {
            return name;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public String getServerCertificate() {
            return serverCertificate;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ArmadaClusterInfoProvider.class.getName());
    private static final ConcurrentHashMap<String, String> lastConfigPaths = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Map<String, ClusterInfo>> clusterInfoMap = new ConcurrentHashMap<>();

    private ArmadaClusterInfoProvider() {}

    public static ClusterInfo resolveClusterInfo(ArmadaCloud cloud, String clusterName) {
        var clusterInfos = clusterInfoMap.getOrDefault(cloud.getDisplayName(), Collections.emptyMap());
        var clusterInfo = clusterInfos.get(clusterName);
        if (clusterInfo == null) {
            throw new IllegalStateException("Unable to resolve cluster info for " + clusterName);
        }
        return clusterInfo;
    }

    private static void reconfigure(List<ArmadaCloud> clouds) {
        var keys = new HashSet<>(clusterInfoMap.keySet());
        for (var cloud : clouds) {
            var displayName = cloud.getDisplayName();
            try {
                if(!StringUtils.equals(cloud.getArmadaClusterConfigPath(), lastConfigPaths.get(displayName))) {
                    LOGGER.info("Updating cluster info for " + displayName);
                    clusterInfoMap.put(displayName, parse(cloud.getArmadaClusterConfigPath()));
                    lastConfigPaths.put(displayName, cloud.getArmadaClusterConfigPath());
                    LOGGER.info("Updated cluster info for cloud " + cloud.getDisplayName());
                }
            } catch(Throwable e) {
                LOGGER.log(Level.SEVERE, "Failed to update cluster info for cloud " + displayName, e);
            }
            keys.remove(displayName);
        }
        for(var cloudName : keys) {
            clusterInfoMap.remove(cloudName);
            lastConfigPaths.remove(cloudName);
        }
    }

    private static Map<String, ClusterInfo> parse(String configPath) {
        var factory = DocumentBuilderFactory.newInstance();
        try {
            var builder = factory.newDocumentBuilder();

            Document doc = builder.parse(configPath);
            doc.getDocumentElement().normalize();

            Map<String, ClusterInfo> clusterMap = new HashMap<>();

            var clusterList = doc.getElementsByTagName("cluster");
            for (int i = 0; i < clusterList.getLength(); i++) {
                var clusterNode = clusterList.item(i);

                if (clusterNode.getNodeType() == Node.ELEMENT_NODE) {
                    var clusterElement = (Element) clusterNode;

                    var name = clusterElement.getElementsByTagName("name").item(0).getTextContent();
                    var url = clusterElement.getElementsByTagName("url").item(0).getTextContent();
                    String certData = null;
                    if (clusterElement.getElementsByTagName("cert_data").getLength() == 1) {
                        certData = clusterElement.getElementsByTagName("cert_data").item(0).getTextContent();
                    }

                    clusterMap.put(name, new ClusterInfo(name, url, certData));
                }
            }

            LOGGER.info("Loaded "+ clusterMap.size() + " clusters from " + configPath);

            return clusterMap;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
