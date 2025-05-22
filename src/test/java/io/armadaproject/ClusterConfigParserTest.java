package io.armadaproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

class ClusterConfigParserTest {

  private static Path TMP_DIR;
  private Path tmpFile;
  private static final String TMP_DIR_NAME = "cluster_config_parser_test";

  @BeforeAll
  static void beforeAll() throws IOException {
    TMP_DIR = Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")),
        TMP_DIR_NAME);
  }

  @BeforeEach
  void beforeEach() {
    tmpFile = TMP_DIR.resolve("clusters.xml");
  }

  @AfterEach
  void afterEach() throws IOException {
    Files.deleteIfExists(tmpFile);
  }

  /*
  @ParameterizedTest
  @MethodSource("provideTestCases")
  void testParse(String xmlContent, Map<String, String> expectedMap) throws Exception {
    Files.writeString(tmpFile, xmlContent);

    Map<String, String> clusterMap = ClusterConfigParser.parse(tmpFile.toString());

    assertEquals(expectedMap.size(), clusterMap.size());
    expectedMap.forEach((key, value) -> assertEquals(value, clusterMap.get(key)));
  }
   */

  private static Stream<Object[]> provideTestCases() {
    return Stream.of(
        new Object[]{
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<clusters>\n</clusters>",
            Map.of()
        },
        new Object[]{
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<clusters>\n" +
                "  <cluster>\n" +
                "    <name>key1</name>\n" +
                "    <url>value1</url>\n" +
                "  </cluster>\n" +
                "</clusters>",
            Map.of("key1", "value1")
        },
        new Object[]{
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<clusters>\n" +
                "  <cluster>\n" +
                "    <name>key1</name>\n" +
                "    <url>value1</url>\n" +
                "  </cluster>\n" +
                "  <cluster>\n" +
                "    <name>key2</name>\n" +
                "    <url>value2</url>\n" +
                "  </cluster>\n" +
                "</clusters>",
            Map.of("key1", "value1", "key2", "value2")
        }
    );
  }

  @Test
  void testParseWrongXml() throws IOException {
    String invalidXmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<clusters>\n" +
        "  <cluster>\n" +
        "    <name>key1</name>\n" +
        "    <url>value1</url>\n" +
        "  </cluster>\n"; // Missing closing tags for <clusters> and <cluster>

    Files.writeString(tmpFile, invalidXmlContent);

    assertThrows(SAXException.class, () -> ClusterConfigParser.parse(tmpFile.toString()));
  }

}