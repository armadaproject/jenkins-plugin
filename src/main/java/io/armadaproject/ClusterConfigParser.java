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

  public static Map<String, String> parse(String configPath) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(configPath);

    doc.getDocumentElement().normalize();

    Map<String, String> clusterMap = new HashMap<>();

    NodeList clusterList = doc.getElementsByTagName("cluster");

    for (int i = 0; i < clusterList.getLength(); i++) {
      Node clusterNode = clusterList.item(i);

      if (clusterNode.getNodeType() == Node.ELEMENT_NODE) {
        Element clusterElement = (Element) clusterNode;

        String name = clusterElement.getElementsByTagName("name").item(0).getTextContent();
        String url = clusterElement.getElementsByTagName("url").item(0).getTextContent();

        clusterMap.put(name, url);
      }
    }

    return clusterMap;
  }

}
