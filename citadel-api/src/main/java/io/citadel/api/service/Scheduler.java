package io.citadel.api.service;

/**
 * Provides delayed and periodic task execution for plugins and internal use.
 *
 * <p>Tasks execute on a dedicated thread pool managed by the core. Plugins should use this service
 * for all asynchronous work instead of creating their own threads.
 *
 * <p>This is an empty contract in the current milestone. Methods will be added when the scheduler
 * is implemented (see ROADMAP).
 *
 * <p>Thread safety: implementations must be thread-safe.
 */
public interface Scheduler extends Service {}
