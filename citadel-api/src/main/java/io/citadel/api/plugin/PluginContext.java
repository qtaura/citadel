package io.citadel.api.plugin;

import io.citadel.api.service.Logger;
import io.citadel.api.service.Service;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Provides a plugin with access to core services and facilities.
 *
 * <p>Each loaded plugin receives its own {@code PluginContext} instance. The context is the sole
 * gateway through which plugins interact with the Citadel core. Plugins must never reference
 * internal core packages directly.
 *
 * <p>This interface is <b>not</b> intended to be implemented by plugins. The core provides the
 * implementation.
 */
public interface PluginContext {

  /**
   * Retrieves a core service by its interface type.
   *
   * <p>Returns an empty {@link Optional} if the requested service is not available. This can happen
   * when the service is optional (e.g., the dashboard services when the dashboard module is not
   * loaded).
   *
   * <p>Usage:
   *
   * <pre>{@code
   * context.getService(Scheduler.class).ifPresent(scheduler -> scheduler.runTask(...));
   * }</pre>
   *
   * @param <T> the service type
   * @param type the service interface class
   * @return an {@link Optional} containing the service, or empty if not available
   * @throws NullPointerException if {@code type} is null
   */
  <T extends Service> Optional<T> getService(Class<T> type);

  /**
   * Returns a logger for this plugin.
   *
   * <p>The returned logger automatically prefixes messages with the plugin name. Log output is
   * routed to the core logging infrastructure (console, file, and eventually the dashboard).
   *
   * @return a plugin-scoped logger
   */
  Logger getLogger();

  /**
   * Returns metadata about the plugin associated with this context.
   *
   * @return plugin metadata (never null)
   */
  PluginMetadata getPluginMetadata();

  /**
   * Returns the plugin's private data directory.
   *
   * <p>This directory is created by the core before {@link Plugin#onLoad(PluginContext)} is called.
   * Plugins should use this directory to store configuration files, cached data, persistent state,
   * or any other files they need. The directory is namespaced by plugin name and located within the
   * Citadel data directory.
   *
   * <p>Plugins must not assume this directory is empty on startup. It may contain files from
   * previous runs. Plugins must not write files outside this directory.
   *
   * @return the absolute path to the plugin's data directory (never null; directory is guaranteed
   *     to exist)
   */
  Path getDataFolder();
}
