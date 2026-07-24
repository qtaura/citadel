package io.citadel.core.logging;

import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Central logging service that creates named loggers, manages Logback configuration, and integrates
 * with the Citadel configuration system.
 *
 * <p>This is an internal class — it is not registered in the {@link
 * io.citadel.core.bootstrap.ServiceRegistry} as a {@link io.citadel.api.service.Service}. Instead,
 * individual {@link Logger} instances (root logger, plugin loggers) are registered where
 * appropriate.
 *
 * <p>Thread safety: thread-safe.
 */
public final class LoggingService implements ConfigurationListener {

  private final ConcurrentMap<String, Logger> loggers;

  public LoggingService() {
    this.loggers = new ConcurrentHashMap<>();
  }

  /**
   * Initializes the logging infrastructure with the given configuration.
   *
   * @param config the Citadel configuration (not null)
   */
  public void initialize(Configuration config) {
    ConfigurationSection loggingConfig = config.getSection("logging");
    LogbackConfigurer.initialize(loggingConfig);
  }

  /**
   * Returns the root logger named {@code Citadel}.
   *
   * @return the root logger (never null)
   */
  public Logger getRootLogger() {
    return getOrCreateLogger("Citadel");
  }

  /**
   * Returns a logger for the given name.
   *
   * @param name the logger name
   * @return a logger (never null)
   */
  public Logger getLogger(String name) {
    return getOrCreateLogger(name);
  }

  /**
   * Returns a logger namespaced for a plugin.
   *
   * @param pluginName the plugin name
   * @return a plugin-scoped logger (never null)
   */
  public Logger getPluginLogger(String pluginName) {
    return getOrCreateLogger("citadel.plugin." + pluginName);
  }

  /** Shuts down the Logback context, flushing all pending log events. */
  public void shutdown() {
    LogbackConfigurer.shutdown();
  }

  @Override
  public void onReload(Configuration config) {
    ConfigurationSection loggingConfig = config.getSection("logging");
    LogbackConfigurer.reconfigure(loggingConfig);
  }

  private Logger getOrCreateLogger(String name) {
    return loggers.computeIfAbsent(
        name,
        n -> {
          org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(n);
          return new Slf4jLogger(slf4jLogger);
        });
  }
}
