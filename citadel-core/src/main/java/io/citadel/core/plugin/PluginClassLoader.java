package io.citadel.core.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Isolated classloader for a single plugin JAR.
 *
 * <p>Each plugin is loaded in its own {@code PluginClassLoader} instance so that plugins cannot see
 * each other's classes. The parent classloader is the one that loaded the {@link
 * io.citadel.api.plugin.Plugin} interface, which provides access to the public API without exposing
 * internal core packages intended for plugin consumption.
 */
public final class PluginClassLoader extends URLClassLoader {

  static {
    registerAsParallelCapable();
  }

  private final PluginDescriptor pluginDescriptor;

  /**
   * @param jarPath path to the plugin JAR
   * @param parent parent classloader (typically the classloader that loaded the API)
   */
  public PluginClassLoader(Path jarPath, ClassLoader parent) {
    super(new URL[] {toURL(jarPath)}, parent);
    this.pluginDescriptor = null;
  }

  PluginClassLoader(Path jarPath, ClassLoader parent, PluginDescriptor descriptor) {
    super(new URL[] {toURL(jarPath)}, parent);
    this.pluginDescriptor = descriptor;
  }

  private static URL toURL(Path jarPath) {
    try {
      return jarPath.toUri().toURL();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JAR path: " + jarPath, e);
    }
  }

  /** Returns the descriptor of the plugin loaded by this classloader, if known. */
  public PluginDescriptor getPluginDescriptor() {
    return pluginDescriptor;
  }
}
