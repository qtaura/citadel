package io.citadel.api.service;

/**
 * Provides configuration access for the core and plugins.
 *
 * <p>This service manages hierarchical configuration — loading, validation, type-safe accessors,
 * plugin-namespaced sections, and programmatic reload. Configuration values are accessed through
 * typed getters using dot-separated paths such as {@code "accounts.default.proxy.port"}.
 *
 * <p>Plugins obtain their own isolated namespace through {@link #getPluginSection(String)}, which
 * transparently maps to the {@code plugins.<pluginName>} section of the global configuration file.
 *
 * <p>Thread safety: implementations must be thread-safe. Configuration may be read from any thread.
 * The configuration snapshot is immutable after loading; a reload atomically replaces the in-memory
 * tree and notifies registered listeners.
 *
 * @see ConfigurationSection
 * @see ConfigurationListener
 */
public interface Configuration extends Service, ConfigurationSection {

  /**
   * Returns the root configuration section.
   *
   * @return the root section (never null)
   */
  ConfigurationSection getRoot();

  /**
   * Returns the configuration section for a specific plugin.
   *
   * <p>Plugin sections live under the {@code plugins.<pluginName>} path. If the section does not
   * exist in the configuration file, an empty section is returned — plugins can always read their
   * own configuration namespace.
   *
   * @param pluginName the plugin name (as declared in {@link
   *     io.citadel.api.plugin.PluginMetadata#name()})
   * @return the plugin's configuration section (never null)
   */
  ConfigurationSection getPluginSection(String pluginName);

  /**
   * Reloads the configuration from disk.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Re-reads the configuration file
   *   <li>Validates all values
   *   <li>Atomically replaces the in-memory configuration snapshot
   *   <li>Notifies all registered {@link ConfigurationListener listeners}
   * </ol>
   *
   * <p>During reload, concurrent readers continue to see the previous snapshot until the swap is
   * complete.
   *
   * @throws ConfigurationException if the configuration file is invalid or cannot be read
   */
  void reload();

  /**
   * Registers a listener that is notified after each successful reload.
   *
   * @param listener the listener to register
   */
  void addListener(ConfigurationListener listener);

  /**
   * Removes a previously registered listener.
   *
   * @param listener the listener to remove
   */
  void removeListener(ConfigurationListener listener);
}
