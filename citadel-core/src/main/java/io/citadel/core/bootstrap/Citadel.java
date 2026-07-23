package io.citadel.core.bootstrap;

import java.io.PrintStream;

/**
 * Main entry point for the Citadel server.
 *
 * <p>This class handles CLI arguments and delegates the bootstrap sequence to {@link Bootstrap}.
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
    try {
      if (handleHelpOrVersion(args)) {
        return;
      }
    } catch (IllegalArgumentException e) {
      System.exit(1);
    }

    new Bootstrap().start();
  }

  /**
   * Handles --help and --version arguments. Prints usage or version and returns true if the
   * application should exit after this call.
   *
   * @param args command-line arguments
   * @return true if the caller should exit immediately
   */
  static boolean handleHelpOrVersion(String[] args) {
    for (String arg : args) {
      switch (arg) {
        case "--help":
        case "-h":
          printUsage(System.out);
          return true;
        case "--version":
        case "-v":
          System.out.println(NAME + " v" + VERSION);
          return true;
        default:
          System.err.println("[" + NAME + "] Unknown argument: " + arg);
          printUsage(System.err);
          throw new IllegalArgumentException("Unknown argument: " + arg);
      }
    }
    return false;
  }

  private static void printUsage(PrintStream out) {
    out.println("Usage: java -jar citadel-core.jar [options]");
    out.println("Options:");
    out.println("  --help, -h      Print this help message and exit");
    out.println("  --version, -v   Print version information and exit");
  }
}
