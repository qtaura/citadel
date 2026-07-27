package io.citadel.core.plugin;

import io.citadel.api.plugin.Plugin;
import io.citadel.api.plugin.PluginMetadata;
import java.nio.file.Path;

/**
 * Immutable record of metadata and runtime handle for a discovered plugin.
 *
 * <p>Populated during the discovery phase before the plugin class is loaded.
 */
public final class PluginDescriptor {

  private final String name;
  private final String version;
  private final String apiVersion;
  private final String mainClassName;
  private final Path jarPath;
  private final PluginMetadata annotation;

  private Class<? extends Plugin> mainClass;
  private Plugin instance;
  private PluginState state;
  private PluginClassLoader classLoader;

  public PluginDescriptor(
      String name,
      String version,
      String apiVersion,
      String mainClassName,
      Path jarPath,
      PluginMetadata annotation) {
    this.name = name;
    this.version = version;
    this.apiVersion = apiVersion;
    this.mainClassName = mainClassName;
    this.jarPath = jarPath;
    this.annotation = annotation;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public String getMainClassName() {
    return mainClassName;
  }

  public Path getJarPath() {
    return jarPath;
  }

  public PluginMetadata getAnnotation() {
    return annotation;
  }

  public Class<? extends Plugin> getMainClass() {
    return mainClass;
  }

  public void setMainClass(Class<? extends Plugin> mainClass) {
    this.mainClass = mainClass;
  }

  public Plugin getInstance() {
    return instance;
  }

  public void setInstance(Plugin instance) {
    this.instance = instance;
  }

  public PluginState getState() {
    return state;
  }

  public void setState(PluginState state) {
    this.state = state;
  }

  public PluginClassLoader getClassLoader() {
    return classLoader;
  }

  public void setClassLoader(PluginClassLoader classLoader) {
    this.classLoader = classLoader;
  }
}
