/**
 * Logging infrastructure wrapping SLF4J and Logback.
 *
 * <p>All core and plugin logging flows through this package. The {@link
 * io.citadel.api.service.Logger} interface in {@code citadel-api} defines the public contract;
 * {@link io.citadel.core.logging.Slf4jLogger} implements it by wrapping an SLF4J logger. {@link
 * io.citadel.core.logging.LogbackConfigurer} programmatically configures Logback (console appender,
 * rolling file appender) from the {@code logging} section of the Citadel configuration.
 *
 * <p>Logback-specific APIs never leak outside this package. Core subsystems and plugins interact
 * only with the {@code Logger} interface.
 */
package io.citadel.core.logging;
