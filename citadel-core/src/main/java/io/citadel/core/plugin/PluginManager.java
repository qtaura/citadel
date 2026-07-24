package io.citadel.core.plugin;

import io.citadel.api.plugin.Plugin;
import io.citadel.api.plugin.PluginContext;
import io.citadel.api.service.Logger;
import io.citadel.core.bootstrap.ServiceRegistry;
import io.citadel.core.logging.LoggingService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages plugin discovery, loading, enabling, and disabling.
 *
 * <p>Lifecycle sequence:
 *
 * <ol>
 *   <li>{@link #loadPlugins()} — discover JARs, create classloaders, instantiate plugins, call
 *       {@link Plugin#onLoad(PluginContext)}
 *   <li>{@link #enablePlugins()} — call {@link Plugin#onEnable()} on every loaded plugin
 *   <li>{@link #disablePlugins()} — call {@link Plugin#onDisable()} on every enabled plugin, then
 *       close classloaders
 * </ol>
 *
 * <p>Error isolation: if any plugin throws during a lifecycle call, the error is logged and the
 * plugin is placed into {@link PluginState#ERROR}. Other plugins continue unaffected.
 */
public final class PluginManager {

  private final ServiceRegistry serviceRegistry;
  private final LoggingService loggingService;
  private final Logger logger;
  private final Path pluginsDirectory;
  private final Path pluginsDataDirectory;
  private final String coreApiVersion;
  private final List<PluginDescriptor> plugins;

  public PluginManager(
      ServiceRegistry serviceRegistry,
      LoggingService loggingService,
      Path pluginsDirectory,
      Path pluginsDataDirectory,
      String coreApiVersion) {
    this.serviceRegistry = serviceRegistry;
    this.loggingService = loggingService;
    this.logger = loggingService.getLogger("PluginManager");
    this.pluginsDirectory = pluginsDirectory;
    this.pluginsDataDirectory = pluginsDataDirectory;
    this.coreApiVersion = coreApiVersion;
    this.plugins = new ArrayList<>();
  }

  /** Scans, loads, and initializes all plugins. Returns the list of descriptors. */
  public List<PluginDescriptor> loadPlugins() {
    if (!Files.isDirectory(pluginsDirectory)) {
      logger.info("Plugins directory {} does not exist; skipping", pluginsDirectory);
      return Collections.emptyList();
    }

    PluginScanner scanner = new PluginScanner(pluginsDirectory);
    List<PluginDescriptor> discovered = scanner.scan(coreApiVersion);

    for (PluginDescriptor descriptor : discovered) {
      loadPlugin(descriptor);
    }

    return Collections.unmodifiableList(new ArrayList<>(plugins));
  }

  /** Calls {@link Plugin#onEnable()} on every loaded plugin. */
  public void enablePlugins() {
    for (PluginDescriptor descriptor : plugins) {
      if (descriptor.getState() != PluginState.LOADED) {
        continue;
      }
      enablePlugin(descriptor);
    }
  }

  /** Calls {@link Plugin#onDisable()} on every enabled plugin and closes classloaders. */
  public void disablePlugins() {
    List<PluginDescriptor> reversed = new ArrayList<>(plugins);
    Collections.reverse(reversed);
    for (PluginDescriptor descriptor : reversed) {
      if (descriptor.getState() == PluginState.ENABLED) {
        disablePlugin(descriptor);
      }
    }
    for (PluginDescriptor descriptor : plugins) {
      if (descriptor.getClassLoader() != null) {
        closeQuietly(descriptor.getClassLoader());
      }
    }
    plugins.clear();
  }

  public List<PluginDescriptor> getPlugins() {
    return Collections.unmodifiableList(plugins);
  }

  private void loadPlugin(PluginDescriptor descriptor) {
    plugins.add(descriptor);
    logger.info("Plugin {} v{} ...", descriptor.getName(), descriptor.getVersion());

    try {
      ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
      @SuppressWarnings("PMD.CloseResource")
      PluginClassLoader classLoader = new PluginClassLoader(descriptor.getJarPath(), parentLoader);
      descriptor.setClassLoader(classLoader);

      @SuppressWarnings("unchecked")
      Class<? extends Plugin> mainClass =
          (Class<? extends Plugin>) Class.forName(descriptor.getMainClassName(), true, classLoader);
      descriptor.setMainClass(mainClass);

      Plugin instance = mainClass.getDeclaredConstructor().newInstance();
      descriptor.setInstance(instance);

      Path dataFolder = ensureDataFolder(descriptor.getName());
      Logger pluginLogger = loggingService.getPluginLogger(descriptor.getName());
      PluginContext context =
          new CorePluginContext(
              serviceRegistry, pluginLogger, descriptor.getAnnotation(), dataFolder);

      instance.onLoad(context);
      descriptor.setState(PluginState.LOADED);

      logger.info("Plugin {} v{} loaded", descriptor.getName(), descriptor.getVersion());
    } catch (Exception e) {
      logger.error("Failed to load plugin {}: {}", descriptor.getName(), e.getMessage());
      logger.error("Plugin load error details", e);
      descriptor.setState(PluginState.ERROR);
    }
  }

  private void enablePlugin(PluginDescriptor descriptor) {
    logger.info("Enabling plugin {} ...", descriptor.getName());
    try {
      descriptor.getInstance().onEnable();
      descriptor.setState(PluginState.ENABLED);
      logger.info("Plugin {} enabled", descriptor.getName());
    } catch (Exception e) {
      logger.error(
          "Plugin {} threw exception in onEnable: {}", descriptor.getName(), e.getMessage());
      logger.error("Plugin enable error details", e);
      descriptor.setState(PluginState.ERROR);
    }
  }

  private void disablePlugin(PluginDescriptor descriptor) {
    logger.info("Disabling plugin {} ...", descriptor.getName());
    try {
      descriptor.getInstance().onDisable();
      descriptor.setState(PluginState.DISABLED);
      logger.info("Plugin {} disabled", descriptor.getName());
    } catch (Exception e) {
      logger.error(
          "Plugin {} threw exception in onDisable: {}", descriptor.getName(), e.getMessage());
      logger.error("Plugin disable error details", e);
      descriptor.setState(PluginState.ERROR);
    }
  }

  private Path ensureDataFolder(String pluginName) {
    Path folder = pluginsDataDirectory.resolve(pluginName);
    try {
      Files.createDirectories(folder);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create data folder for plugin " + pluginName, e);
    }
    return folder;
  }

  private static void closeQuietly(PluginClassLoader classLoader) {
    try {
      classLoader.close();
    } catch (IOException e) {
      // Ignore
    }
  }
}
