/**
 * Public plugin API for Citadel.
 *
 * <p>This module defines the contract between the Citadel core and plugins. All interfaces, events,
 * and data types that plugins use are defined here. The API has zero dependencies on any internal
 * implementation.
 *
 * <p>Plugin developers should only import types from this module and must never depend on internal
 * packages in {@code citadel-core}.
 *
 * <h2>Package Overview</h2>
 *
 * <ul>
 *   <li>{@link io.citadel.api.plugin} — Plugin lifecycle, metadata, and service access
 *   <li>{@link io.citadel.api.event} — Event system for core and plugin communication
 *   <li>{@link io.citadel.api.service} — Core service interfaces (Configuration, Scheduler,
 *       AccountManager, Logger)
 * </ul>
 */
package io.citadel.api;
