package io.citadel.core.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultConfigurationTest {

  @Test
  void generateReturnsNonEmptyString() {
    String yaml = DefaultConfiguration.generate();
    assertNotNull(yaml);
    assertFalse(yaml.isEmpty());
  }

  @Test
  void generateProducesValidYaml() {
    String yaml = DefaultConfiguration.generate();
    YamlConfigurationLoader loader = new YamlConfigurationLoader();
    Map<String, Object> parsed = loader.load(yaml);
    assertNotNull(parsed);
    assertFalse(parsed.isEmpty());
  }

  @Test
  void generatedConfigContainsNetworkingSection() {
    String yaml = DefaultConfiguration.generate();
    YamlConfigurationLoader loader = new YamlConfigurationLoader();
    Map<String, Object> parsed = loader.load(yaml);
    assertNotNull(parsed.get("networking"));
  }

  @Test
  void generatedConfigContainsAuthenticationSection() {
    String yaml = DefaultConfiguration.generate();
    Map<String, Object> parsed = new YamlConfigurationLoader().load(yaml);
    assertNotNull(parsed.get("authentication"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void generatedConfigContainsConnectionSection() {
    String yaml = DefaultConfiguration.generate();
    Map<String, Object> parsed = new YamlConfigurationLoader().load(yaml);
    assertNotNull(parsed.get("connection"));
    assertEquals(10000, ((Map<String, Object>) parsed.get("connection")).get("login_timeout"));
  }

  @Test
  void generatedConfigContainsProxiesSection() {
    String yaml = DefaultConfiguration.generate();
    Map<String, Object> parsed = new YamlConfigurationLoader().load(yaml);
    assertNotNull(parsed.get("proxies"));
  }

  @Test
  void generatedConfigContainsAccountsSection() {
    String yaml = DefaultConfiguration.generate();
    Map<String, Object> parsed = new YamlConfigurationLoader().load(yaml);
    assertNotNull(parsed.get("accounts"));
  }

  @Test
  void generatedConfigContainsLoggingSection() {
    String yaml = DefaultConfiguration.generate();
    Map<String, Object> parsed = new YamlConfigurationLoader().load(yaml);
    assertNotNull(parsed.get("logging"));
  }

  @Test
  void generatedConfigContainsDashboardSection() {
    String yaml = DefaultConfiguration.generate();
    Map<String, Object> parsed = new YamlConfigurationLoader().load(yaml);
    assertNotNull(parsed.get("dashboard"));
  }

  @Test
  void generatedConfigContainsPluginsSection() {
    String yaml = DefaultConfiguration.generate();
    Map<String, Object> parsed = new YamlConfigurationLoader().load(yaml);
    assertNotNull(parsed.get("plugins"));
  }

  @Test
  void generatedConfigHasDefaultValues() {
    String yaml = DefaultConfiguration.generate();
    Map<String, Object> parsed = new YamlConfigurationLoader().load(yaml);
    @SuppressWarnings("unchecked")
    Map<String, Object> logging = (Map<String, Object>) parsed.get("logging");
    assertEquals("INFO", logging.get("level"));
  }
}
