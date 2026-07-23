package io.citadel.core.bootstrap;

import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.Scheduler;
import io.citadel.core.service.NoOpAccountManager;
import io.citadel.core.service.NoOpConfiguration;
import io.citadel.core.service.NoOpScheduler;

/**
 * Owns the Citadel startup sequence.
 *
 * <p>Created by {@link Citadel#main(String[])} after CLI arguments are handled. Owns the {@link
 * Lifecycle} state machine and {@link ServiceRegistry}, and wires them together through the
 * bootstrap sequence.
 */
public final class Bootstrap {

  private static final String NAME = "Citadel";
  private static final String VERSION = "0.1.0-SNAPSHOT";

  private final Lifecycle lifecycle;
  private final ServiceRegistry serviceRegistry;

  public Bootstrap() {
    this.lifecycle = new Lifecycle();
    this.serviceRegistry = new ServiceRegistry();
  }

  /** Runs the full bootstrap sequence. Blocks until shutdown is requested. */
  public void start() {
    registerCoreServices(serviceRegistry);

    System.out.println("[" + NAME + "] Starting " + NAME + " v" + VERSION + " ...");
    lifecycle.start();

    // Plugin loading happens here in Milestone 4.

    lifecycle.finishStartup();
    System.out.println("[" + NAME + "] " + NAME + " v" + VERSION + " started successfully.");

    try {
      lifecycle.awaitShutdown();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    System.out.println("[" + NAME + "] " + NAME + " v" + VERSION + " terminated.");
  }

  /** Exposed for testing. */
  Lifecycle getLifecycle() {
    return lifecycle;
  }

  /** Exposed for testing. */
  ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  private static void registerCoreServices(ServiceRegistry registry) {
    registry.register(Configuration.class, new NoOpConfiguration());
    registry.register(Scheduler.class, new NoOpScheduler());
    registry.register(AccountManager.class, new NoOpAccountManager());
  }
}
