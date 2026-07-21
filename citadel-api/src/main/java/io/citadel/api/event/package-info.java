/**
 * Event system for core and plugin communication.
 *
 * <p>This package provides the event-driven communication infrastructure. Events extend {@link
 * io.citadel.api.event.Event} and are published through the {@link io.citadel.api.event.EventBus}.
 * Handlers implement the {@link io.citadel.api.event.EventHandler} functional interface and
 * subscribe to specific event types.
 *
 * <p>Two delivery modes are available:
 *
 * <ul>
 *   <li><b>Synchronous</b> — publisher blocks until all handlers complete. Used when the publisher
 *       needs handler results (e.g., packet modification).
 *   <li><b>Asynchronous</b> — publisher returns immediately; handlers run on a thread pool. The
 *       default for most events.
 * </ul>
 *
 * <p>Exception isolation is guaranteed: a failing handler does not prevent other handlers from
 * receiving the event.
 */
package io.citadel.api.event;
