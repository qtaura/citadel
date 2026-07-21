package io.citadel.api.plugin;

/**
 * Represents a Citadel plugin.
 *
 * <p>Every plugin JAR must contain exactly one class implementing this interface. The implementing
 * class must be annotated with {@link PluginMetadata @PluginMetadata} to provide metadata (name,
 * version, API version requirement).
 *
 * <p>Lifecycle calls happen in this order:
 *
 * <ol>
 *   <li>{@link #onLoad(PluginContext)} — Called after the plugin is first loaded and validated.
 *       Plugins should perform lightweight initialization here. No other plugins are guaranteed to
 *       be loaded yet.
 *   <li>{@link #onEnable()} — Called after all plugins have been loaded. All core services are
 *       available. Plugins should start their primary work here.
 *   <li>{@link #onDisable()} — Called during shutdown or when the plugin is explicitly disabled.
 *       Plugins should release resources, cancel tasks, and save state.
 * </ol>
 *
 * <p>All lifecycle methods are called from the main application thread. Implementations must not
 * block indefinitely. Long-running work should be offloaded to the {@link
 * io.citadel.api.service.Scheduler} service.
 *
 * <p>If any lifecycle method throws an exception, it is logged and the plugin is placed into an
 * error state. A thrown exception in {@code onLoad} or {@code onEnable} prevents {@code onEnable}
 * and {@code onDisable} from being called, respectively.
 */
public interface Plugin {

  /**
   * Called when the plugin is first loaded.
   *
   * <p>This is the earliest initialization point. The plugin receives its {@link PluginContext} and
   * can perform basic setup. Core services may not all be available yet.
   *
   * @param context the plugin context for accessing core services and the plugin logger
   */
  void onLoad(PluginContext context);

  /**
   * Called after all plugins have been loaded.
   *
   * <p>Core services are fully available. The plugin should start its primary functionality here.
   * This is the appropriate place to register event handlers, schedule recurring tasks, and read
   * plugin configuration.
   */
  void onEnable();

  /**
   * Called during shutdown or when the plugin is disabled.
   *
   * <p>The plugin should release all resources, cancel scheduled tasks, unregister event handlers,
   * and persist any state that should survive a restart. After this method returns, the plugin must
   * not perform any further work.
   */
  void onDisable();
}
