package io.citadel.core.plugin;

import io.citadel.api.plugin.PluginContext;
import io.citadel.api.plugin.PluginMetadata;
import io.citadel.api.service.Logger;
import io.citadel.api.service.Service;
import io.citadel.core.bootstrap.ServiceRegistry;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Core implementation of {@link PluginContext}.
 *
 * <p>Each loaded plugin receives its own instance. Service lookup is delegated to the shared {@link
 * ServiceRegistry}.
 */
public final class CorePluginContext implements PluginContext {

  private final ServiceRegistry serviceRegistry;
  private final Logger logger;
  private final PluginMetadata metadata;
  private final Path dataFolder;

  public CorePluginContext(
      ServiceRegistry serviceRegistry, Logger logger, PluginMetadata metadata, Path dataFolder) {
    this.serviceRegistry = serviceRegistry;
    this.logger = logger;
    this.metadata = metadata;
    this.dataFolder = dataFolder;
  }

  @Override
  public <T extends Service> Optional<T> getService(Class<T> type) {
    return serviceRegistry.get(type);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public PluginMetadata getPluginMetadata() {
    return metadata;
  }

  @Override
  public Path getDataFolder() {
    return dataFolder;
  }
}
