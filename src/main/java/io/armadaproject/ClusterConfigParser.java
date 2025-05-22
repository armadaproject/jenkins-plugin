package io.armadaproject;

import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ClusterConfigParser {

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

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + apiUrl.hashCode();
      result = 31 * result + (serverCertificate != null ? serverCertificate.hashCode() : 0);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      ClusterInfo that = (ClusterInfo) obj;
      return name.equals(that.name) &&
          apiUrl.equals(that.apiUrl) &&
          ((serverCertificate == null && that.serverCertificate == null) ||
              (serverCertificate != null && serverCertificate.equals(that.serverCertificate)));
    }
  }

  public static Map<String, ClusterInfo> parse(String configPath) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(configPath);

    doc.getDocumentElement().normalize();

    Map<String, ClusterInfo> clusterMap = new HashMap<>();

    NodeList clusterList = doc.getElementsByTagName("cluster");

    for (int i = 0; i < clusterList.getLength(); i++) {
      Node clusterNode = clusterList.item(i);

      if (clusterNode.getNodeType() == Node.ELEMENT_NODE) {
        Element clusterElement = (Element) clusterNode;

        String name = clusterElement.getElementsByTagName("name").item(0).getTextContent();
        String url = clusterElement.getElementsByTagName("url").item(0).getTextContent();
        String certData = null;
        if (clusterElement.getElementsByTagName("cert_data").getLength() == 1) {
          certData = clusterElement.getElementsByTagName("cert_data").item(0).getTextContent();
        }

        clusterMap.put(name, new ClusterInfo(name, url, certData));
      }
    }

    return clusterMap;
  }

}
