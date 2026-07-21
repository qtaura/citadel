package io.citadel.api.service;

/**
 * Provides logging output for plugins and core subsystems.
 *
 * <p>Each plugin receives its own logger instance (through {@link
 * io.citadel.api.plugin.PluginContext#getLogger()}) that automatically prefixes messages with the
 * plugin name. Log output is routed to the core logging infrastructure (console, file, and
 * eventually the dashboard).
 *
 * <p>This is an empty contract in the current milestone. Methods will be added in Milestone 6
 * (Logging).
 *
 * <p>Thread safety: implementations must be thread-safe.
 */
public interface Logger extends Service {}
