package io.citadel.core.config;

import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Citadel configuration service implementation.
 *
 * <p>Loads a YAML configuration file from disk, provides typed accessors, supports
 * plugin-namespaced sections, and implements programmatic reload with listener notification.
 *
 * <p>Thread safety: all reads delegate to an immutable snapshot stored in an {@link
 * AtomicReference}. Reads are lock-free and wait-free. A reload creates a new snapshot and
 * atomically swaps the reference. Concurrent readers continue to see the previous snapshot until
 * the swap completes.
 */
public final class CitadelConfiguration implements Configuration {

  private final Path configFile;
  private final AtomicReference<ConfigurationSection> rootRef;
  private final List<ConfigurationListener> listeners;
  private final YamlConfigurationLoader loader;

  /**
   * @param configFile path to the YAML configuration file
   */
  public CitadelConfiguration(Path configFile) {
    this.configFile = configFile;
    this.rootRef = new AtomicReference<>(new ConfigurationSectionImpl(Map.of()));
    this.listeners = new CopyOnWriteArrayList<>();
    this.loader = new YamlConfigurationLoader();
  }

  /**
   * Loads the configuration from disk. If the file does not exist, the default configuration is
   * generated and saved to disk, then loaded.
   *
   * @throws ConfigurationException if the file exists but cannot be parsed
   */
  public void load() {
    try {
      if (!Files.exists(configFile)) {
        generateDefaultConfig();
      }
      Map<String, Object> parsed = loadFile();
      rootRef.set(new ConfigurationSectionImpl(deepUnmodifiable(parsed)));
    } catch (IOException e) {
      throw new io.citadel.api.service.ConfigurationException(
          "(file)", "Failed to read configuration file: " + configFile, e);
    } catch (RuntimeException e) {
      throw new io.citadel.api.service.ConfigurationException(
          "(file)", "Failed to parse configuration file: " + configFile, e);
    }
  }

  // ---- ConfigurationSection implementation (delegates to root) ----

  @Override
  public String getString(String path) {
    return root().getString(path);
  }

  @Override
  public String getString(String path, String defaultValue) {
    return root().getString(path, defaultValue);
  }

  @Override
  public boolean getBoolean(String path) {
    return root().getBoolean(path);
  }

  @Override
  public boolean getBoolean(String path, boolean defaultValue) {
    return root().getBoolean(path, defaultValue);
  }

  @Override
  public int getInt(String path) {
    return root().getInt(path);
  }

  @Override
  public int getInt(String path, int defaultValue) {
    return root().getInt(path, defaultValue);
  }

  @Override
  public long getLong(String path) {
    return root().getLong(path);
  }

  @Override
  public long getLong(String path, long defaultValue) {
    return root().getLong(path, defaultValue);
  }

  @Override
  public double getDouble(String path) {
    return root().getDouble(path);
  }

  @Override
  public double getDouble(String path, double defaultValue) {
    return root().getDouble(path, defaultValue);
  }

  @Override
  public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
    return root().getEnum(path, enumClass);
  }

  @Override
  public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
    return root().getEnum(path, enumClass, defaultValue);
  }

  @Override
  public List<String> getStringList(String path) {
    return root().getStringList(path);
  }

  @Override
  public <T> List<T> getList(String path) {
    return root().getList(path);
  }

  @Override
  public ConfigurationSection getSection(String path) {
    return root().getSection(path);
  }

  @Override
  public boolean contains(String path) {
    return root().contains(path);
  }

  @Override
  public Set<String> getKeys() {
    return root().getKeys();
  }

  // ---- Configuration-specific methods ----

  @Override
  public ConfigurationSection getRoot() {
    return root();
  }

  @Override
  public ConfigurationSection getPluginSection(String pluginName) {
    return root().getSection("plugins." + pluginName);
  }

  @Override
  public void reload() {
    load();
    for (ConfigurationListener listener : listeners) {
      listener.onReload(this);
    }
  }

  @Override
  public void addListener(ConfigurationListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ConfigurationListener listener) {
    listeners.remove(listener);
  }

  // ---- Internal helpers ----

  private ConfigurationSection root() {
    return rootRef.get();
  }

  private Map<String, Object> loadFile() throws IOException {
    try (InputStream in = Files.newInputStream(configFile)) {
      return loader.load(in);
    }
  }

  private void generateDefaultConfig() throws IOException {
    Files.createDirectories(configFile.getParent());
    String defaultYaml = DefaultConfiguration.generate();
    Files.writeString(configFile, defaultYaml);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> deepUnmodifiable(Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof Map) {
        entry.setValue(deepUnmodifiable((Map<String, Object>) entry.getValue()));
      }
    }
    return Collections.unmodifiableMap(map);
  }
}
