package io.citadel.api.service;

/**
 * Provides logging output for plugins and core subsystems.
 *
 * <p>Each plugin receives its own logger instance (through {@link
 * io.citadel.api.plugin.PluginContext#getLogger()}) that automatically namespaces messages under
 * the plugin name. Log output is routed to the core logging infrastructure (console, file, and
 * eventually the dashboard).
 *
 * <p>Account-aware overloads accept an account name parameter. The implementation uses MDC (Mapped
 * Diagnostic Context) to correlate log entries with accounts — callers should not manually format
 * account names into messages.
 *
 * <p>Thread safety: implementations must be thread-safe.
 */
public interface Logger extends Service {

  // -- Level checks --

  boolean isTraceEnabled();

  boolean isDebugEnabled();

  boolean isInfoEnabled();

  boolean isWarnEnabled();

  boolean isErrorEnabled();

  // -- Trace --

  void trace(String message);

  void trace(String format, Object... args);

  void trace(String message, Throwable throwable);

  void trace(String accountName, String message);

  void trace(String accountName, String format, Object... args);

  void trace(String accountName, String message, Throwable throwable);

  // -- Debug --

  void debug(String message);

  void debug(String format, Object... args);

  void debug(String message, Throwable throwable);

  void debug(String accountName, String message);

  void debug(String accountName, String format, Object... args);

  void debug(String accountName, String message, Throwable throwable);

  // -- Info --

  void info(String message);

  void info(String format, Object... args);

  void info(String message, Throwable throwable);

  void info(String accountName, String message);

  void info(String accountName, String format, Object... args);

  void info(String accountName, String message, Throwable throwable);

  // -- Warn --

  void warn(String message);

  void warn(String format, Object... args);

  void warn(String message, Throwable throwable);

  void warn(String accountName, String message);

  void warn(String accountName, String format, Object... args);

  void warn(String accountName, String message, Throwable throwable);

  // -- Error --

  void error(String message);

  void error(String format, Object... args);

  void error(String message, Throwable throwable);

  void error(String accountName, String message);

  void error(String accountName, String format, Object... args);

  void error(String accountName, String message, Throwable throwable);
}
