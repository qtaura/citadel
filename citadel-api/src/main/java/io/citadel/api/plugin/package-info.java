/**
 * Plugin lifecycle, metadata, and service access.
 *
 * <p>This package defines the contract every plugin must implement ({@link
 * io.citadel.api.plugin.Plugin}), the annotation used to declare plugin metadata ({@link
 * io.citadel.api.plugin.PluginMetadata @PluginMetadata}), and the context through which plugins
 * access core services ({@link io.citadel.api.plugin.PluginContext}).
 *
 * <p>All plugin main classes must:
 *
 * <ol>
 *   <li>Be annotated with {@code @PluginMetadata(name, version, apiVersion)}
 *   <li>Implement {@link io.citadel.api.plugin.Plugin}
 *   <li>Be the only class in the JAR implementing {@code Plugin}
 * </ol>
 */
package io.citadel.api.plugin;
