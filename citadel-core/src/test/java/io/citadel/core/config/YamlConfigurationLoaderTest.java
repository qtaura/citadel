package io.citadel.core.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class YamlConfigurationLoaderTest {

  private final YamlConfigurationLoader loader = new YamlConfigurationLoader();

  @Test
  void loadEmptyStringReturnsEmptyMap() {
    Map<String, Object> result = loader.load("");
    assertTrue(result.isEmpty());
  }

  @Test
  void loadNullYamlReturnsEmptyMap() {
    Map<String, Object> result = loader.load("null");
    assertTrue(result.isEmpty());
  }

  @Test
  void loadSimpleKeyValue() {
    Map<String, Object> result = loader.load("key: value");
    assertEquals("value", result.get("key"));
  }

  @Test
  void loadNestedStructure() {
    Map<String, Object> result =
        loader.load("networking:\n  server:\n    host: localhost\n    port: 25565");
    assertNotNull(result.get("networking"));
    @SuppressWarnings("unchecked")
    Map<String, Object> networking = (Map<String, Object>) result.get("networking");
    @SuppressWarnings("unchecked")
    Map<String, Object> server = (Map<String, Object>) networking.get("server");
    assertEquals("localhost", server.get("host"));
    assertEquals(25565, server.get("port"));
  }

  @Test
  void loadListValue() {
    Map<String, Object> result = loader.load("items:\n  - a\n  - b\n  - c");
    assertNotNull(result.get("items"));
    @SuppressWarnings("unchecked")
    List<String> items = (List<String>) result.get("items");
    assertEquals(List.of("a", "b", "c"), items);
  }

  @Test
  void loadBooleanAndNumericTypes() {
    Map<String, Object> result = loader.load("enabled: true\ncount: 42\npi: 3.14\nname: test");
    assertEquals(true, result.get("enabled"));
    assertEquals(42, result.get("count"));
    assertEquals(3.14, result.get("pi"));
    assertEquals("test", result.get("name"));
  }

  @Test
  void loadFromReader() {
    Map<String, Object> result = loader.load(new StringReader("key: value"));
    assertEquals("value", result.get("key"));
  }

  @Test
  void loadListAtRootReturnsEmptyMap() {
    Map<String, Object> result = loader.load("- one\n- two");
    assertTrue(result.isEmpty());
  }

  @Test
  void loadStringWithComments() {
    Map<String, Object> result = loader.load("# comment\nkey: value\n# another comment");
    assertEquals("value", result.get("key"));
  }
}
