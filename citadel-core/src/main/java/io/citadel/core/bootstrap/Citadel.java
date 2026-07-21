package io.citadel.core.bootstrap;

import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.Scheduler;
import io.citadel.core.service.NoOpAccountManager;
import io.citadel.core.service.NoOpConfiguration;
import io.citadel.core.service.NoOpScheduler;

/**
 * Main entry point for the Citadel server.
 *
 * <p>Bootstrap sequence:
 *
 * <ol>
 *   <li>Parse CLI arguments
 *   <li>Print banner / version if requested
 *   <li>Create the {@link Lifecycle} state machine
 *   <li>Create the {@link ServiceRegistry} and register core services
 *   <li>Start the lifecycle ({@link LifecycleState#STARTING})
 *   <li>Complete startup ({@link LifecycleState#RUNNING})
 *   <li>Block on {@link Lifecycle#awaitShutdown()} until the JVM is told to stop
 *   <li>Print termination message and exit 0
 * </ol>
 */
public final class Citadel {

  private static final String NAME = "Citadel";
  private static final String VERSION = "0.1.0-SNAPSHOT";

  private Citadel() {}

  /**
   * Application entry point.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    if (handleHelpOrVersion(args)) {
      return;
    }

    Lifecycle lifecycle = new Lifecycle();
    ServiceRegistry serviceRegistry = new ServiceRegistry();
    registerCoreServices(serviceRegistry);

    System.out.println("[" + NAME + "] Starting " + NAME + " v" + VERSION + " ...");
    lifecycle.start();

    lifecycle.finishStartup();
    System.out.println("[" + NAME + "] " + NAME + " v" + VERSION + " started successfully.");

    try {
      lifecycle.awaitShutdown();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    System.out.println("[" + NAME + "] " + NAME + " v" + VERSION + " terminated.");
    System.exit(0);
  }

  /**
   * Handles --help and --version arguments. Prints usage or version to stdout and returns true if
   * the application should exit after this call.
   *
   * @param args command-line arguments
   * @return true if the caller should exit immediately
   */
  static boolean handleHelpOrVersion(String[] args) {
    for (String arg : args) {
      switch (arg) {
        case "--help":
        case "-h":
          printUsage();
          return true;
        case "--version":
        case "-v":
          System.out.println(NAME + " v" + VERSION);
          return true;
        default:
          System.err.println("[" + NAME + "] Unknown argument: " + arg);
          printUsage();
          System.exit(1);
          return true;
      }
    }
    return false;
  }

  private static void printUsage() {
    System.out.println("Usage: java -jar citadel-core.jar [options]");
    System.out.println("Options:");
    System.out.println("  --help, -h      Print this help message and exit");
    System.out.println("  --version, -v   Print version information and exit");
  }

  private static void registerCoreServices(ServiceRegistry registry) {
    registry.register(Configuration.class, new NoOpConfiguration());
    registry.register(Scheduler.class, new NoOpScheduler());
    registry.register(AccountManager.class, new NoOpAccountManager());
  }
}
