package io.citadel.api.service;

/**
 * Receives notifications when the configuration is reloaded.
 *
 * <p>Listeners are invoked after the new configuration has been loaded, validated, and made
 * available. The provided {@link Configuration} reference reflects the current (post-reload) state.
 */
@FunctionalInterface
public interface ConfigurationListener {

  /**
   * Called after a successful configuration reload.
   *
   * @param configuration the current configuration (post-reload)
   */
  void onReload(Configuration configuration);
}
