/**
 * Core service interfaces for plugin consumption.
 *
 * <p>Services are singleton components managed by the Citadel core. Each service interface extends
 * {@link io.citadel.api.service.Service} and is obtained through {@link
 * io.citadel.api.plugin.PluginContext#getService(Class)}.
 *
 * <p>Service interfaces:
 *
 * <ul>
 *   <li>{@link io.citadel.api.service.Configuration} — hierarchical configuration with typed
 *       accessors, plugin namespaces, and reload support
 *   <li>{@link io.citadel.api.service.ConfigurationSection} — typed accessors for a named
 *       configuration subtree
 *   <li>{@link io.citadel.api.service.ConfigurationListener} — receives notification after
 *       configuration reload
 *   <li>{@link io.citadel.api.service.Scheduler} — delayed and periodic task execution
 *   <li>{@link io.citadel.api.service.AccountManager} — account lifecycle and state tracking
 *       (contract added in Milestone 10)
 *   <li>{@link io.citadel.api.service.Logger} — structured logging (contract added in Milestone 6)
 * </ul>
 *
 * <p>Service interfaces that have not yet been implemented are empty marker interfaces. Their
 * methods are added in the milestone where each service is implemented.
 */
package io.citadel.api.service;
