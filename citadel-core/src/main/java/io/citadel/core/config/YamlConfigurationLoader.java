package io.citadel.core.config;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Parses YAML content into a {@code Map<String, Object>} suitable for {@link
 * ConfigurationSectionImpl}.
 *
 * <p>This class isolates the YAML parsing concern from the rest of the configuration subsystem. If
 * the configuration format changes in the future (e.g., to TOML or HOCON), only this class and
 * {@link DefaultConfiguration} need to change.
 */
final class YamlConfigurationLoader {

  private final Yaml yaml;

  YamlConfigurationLoader() {
    this.yaml = new Yaml();
  }

  /**
   * Parses YAML from an input stream.
   *
   * @param input the YAML input stream
   * @return a flat/deep map representation (never null; empty map for empty input)
   */
  @SuppressWarnings("unchecked")
  Map<String, Object> load(InputStream input) {
    Object parsed = yaml.load(input);
    if (parsed == null) {
      return Map.of();
    }
    if (parsed instanceof Map) {
      return (Map<String, Object>) parsed;
    }
    return Map.of();
  }

  /**
   * Parses YAML from a reader.
   *
   * @param reader the YAML reader
   * @return a flat/deep map representation (never null; empty map for empty input)
   */
  @SuppressWarnings("unchecked")
  Map<String, Object> load(Reader reader) {
    Object parsed = yaml.load(reader);
    if (parsed == null) {
      return Map.of();
    }
    if (parsed instanceof Map) {
      return (Map<String, Object>) parsed;
    }
    return Map.of();
  }

  /**
   * Parses a YAML string.
   *
   * @param yamlContent the YAML string
   * @return a flat/deep map representation (never null; empty map for empty input)
   */
  @SuppressWarnings("unchecked")
  Map<String, Object> load(String yamlContent) {
    Object parsed = yaml.load(yamlContent);
    if (parsed == null) {
      return Map.of();
    }
    if (parsed instanceof Map) {
      return (Map<String, Object>) parsed;
    }
    return Map.of();
  }
}
