package io.citadel.core.bootstrap;

import io.citadel.api.event.EventBus;
import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.Logger;
import io.citadel.api.service.Scheduler;
import io.citadel.core.config.CitadelConfiguration;
import io.citadel.core.event.EventBusImpl;
import io.citadel.core.logging.LoggingService;
import io.citadel.core.plugin.PluginManager;
import io.citadel.core.service.NoOpAccountManager;
import io.citadel.core.service.NoOpScheduler;
import java.nio.file.Path;

/**
 * Owns the Citadel startup sequence.
 *
 * <p>Created by {@link Citadel#main(String[])} after CLI arguments are handled. Owns the {@link
 * Lifecycle} state machine, {@link ServiceRegistry}, {@link LoggingService}, and {@link
 * PluginManager}, and wires them together through the bootstrap sequence.
 */
public final class Bootstrap {

  private static final String NAME = "Citadel";
  private static final String VERSION = "0.1.0-SNAPSHOT";
  private static final String API_VERSION = "0.1.0";

  private final Lifecycle lifecycle;
  private final ServiceRegistry serviceRegistry;
  private final LoggingService loggingService;
  private EventBusImpl eventBus;

  public Bootstrap() {
    this.lifecycle = new Lifecycle();
    this.serviceRegistry = new ServiceRegistry();
    this.loggingService = new LoggingService();
  }

  /** Runs the full bootstrap sequence. Blocks until shutdown is requested. */
  public void start() {
    CitadelConfiguration configuration = new CitadelConfiguration(Path.of("citadel.yml"));
    configuration.load();
    serviceRegistry.register(Configuration.class, configuration);

    loggingService.initialize(configuration);
    Logger rootLogger = loggingService.getRootLogger();
    serviceRegistry.register(Logger.class, rootLogger);
    rootLogger.info("Starting {} v{} ...", NAME, VERSION);

    this.eventBus = new EventBusImpl(rootLogger);
    serviceRegistry.register(EventBus.class, eventBus);
    rootLogger.info("Event bus initialized");

    registerNoOpServices(serviceRegistry);

    lifecycle.start();

    PluginManager pluginManager =
        new PluginManager(
            serviceRegistry,
            loggingService,
            Path.of("plugins"),
            Path.of("plugins-data"),
            API_VERSION);

    pluginManager.loadPlugins();
    pluginManager.enablePlugins();

    // Disable plugins during JVM shutdown to guarantee completion.
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  pluginManager.disablePlugins();
                  eventBus.shutdown();
                  loggingService.shutdown();
                }));

    lifecycle.finishStartup();
    rootLogger.info("{} v{} started successfully.", NAME, VERSION);

    try {
      lifecycle.awaitShutdown();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    eventBus.shutdown();
    loggingService.shutdown();
  }

  /** Exposed for testing. */
  LoggingService getLoggingService() {
    return loggingService;
  }

  /** Exposed for testing. */
  EventBusImpl getEventBus() {
    return eventBus;
  }

  /** Exposed for testing. */
  Lifecycle getLifecycle() {
    return lifecycle;
  }

  /** Exposed for testing. */
  ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  private static void registerNoOpServices(ServiceRegistry registry) {
    registry.register(Scheduler.class, new NoOpScheduler());
    registry.register(AccountManager.class, new NoOpAccountManager());
  }
}
