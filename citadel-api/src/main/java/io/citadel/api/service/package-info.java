/**
 * Core service interfaces for plugin consumption.
 *
 * <p>Services are singleton components managed by the Citadel core. Each service interface extends
 * {@link io.citadel.api.service.Service} and is obtained through {@link
 * io.citadel.api.plugin.PluginContext#getService(Class)}.
 *
 * <p>Current service interfaces:
 *
 * <ul>
 *   <li>{@link io.citadel.api.service.Configuration} — configuration loading, hierarchy, and
 *       validation (contract added in Milestone 5)
 *   <li>{@link io.citadel.api.service.Scheduler} — delayed and periodic task execution
 *   <li>{@link io.citadel.api.service.AccountManager} — account lifecycle and state tracking
 *       (contract added in Milestone 10)
 *   <li>{@link io.citadel.api.service.Logger} — structured logging (contract added in Milestone 6)
 * </ul>
 *
 * <p>Service interfaces are initially empty contracts. Methods are added in the milestone where
 * each service is implemented.
 */
package io.citadel.api.service;
