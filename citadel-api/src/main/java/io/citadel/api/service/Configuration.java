package io.citadel.api.service;

/**
 * Provides configuration access for the core and plugins.
 *
 * <p>This service manages hierarchical configuration merging global defaults, group overrides,
 * account overrides, and plugin-specific sections. Configuration values are typed and validated.
 *
 * <p>This is an empty contract in the current milestone. Methods will be added in Milestone 5
 * (Configuration).
 *
 * <p>Thread safety: implementations must be thread-safe. Configuration may be read from any thread.
 */
public interface Configuration extends Service {}
