package io.citadel.api.service;

/**
 * Marker interface for core services accessible through {@link
 * io.citadel.api.plugin.PluginContext#getService(Class)}.
 *
 * <p>Services are singleton components managed by the core. They provide functionality that plugins
 * can use without depending on internal implementation packages.
 *
 * <p>This interface has no methods. Individual service interfaces extend it to declare their
 * contracts.
 */
public interface Service {}
