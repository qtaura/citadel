package io.citadel.core.plugin;

/**
 * Lifecycle states of a loaded plugin.
 *
 * <p>Transitions:
 *
 * <ul>
 *   <li>{@link #LOADED} → {@link #ENABLED} | {@link #ERROR}
 *   <li>{@link #ENABLED} → {@link #DISABLED} | {@link #ERROR}
 *   <li>{@link #DISABLED} no further transitions
 *   <li>{@link #ERROR} no further transitions
 * </ul>
 */
public enum PluginState {
  LOADED,
  ENABLED,
  DISABLED,
  ERROR
}
