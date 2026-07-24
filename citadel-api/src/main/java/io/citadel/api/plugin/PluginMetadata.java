package io.citadel.api.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares metadata for a Citadel plugin.
 *
 * <p>Every plugin JAR must contain exactly one class annotated with {@code @PluginMetadata}. That
 * class must implement {@link Plugin}. The annotation is read at load time to validate
 * compatibility before the plugin class is instantiated.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * &#64;PluginMetadata(name = "MapArt", version = "1.0.0", apiVersion = "0.1.0")
 * public class MapArtPlugin implements Plugin {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginMetadata {

  /**
   * @return the human-readable plugin name (e.g., "Map Art")
   */
  String name();

  /**
   * @return the plugin version (e.g., "1.0.0")
   */
  String version();

  /**
   * The minimum API version this plugin requires.
   *
   * <p>If the running core provides an API version older than this, the plugin will not be loaded
   * and an error message will be logged explaining the incompatibility.
   *
   * @return the required API version (e.g., "0.1.0")
   */
  String apiVersion();
}
