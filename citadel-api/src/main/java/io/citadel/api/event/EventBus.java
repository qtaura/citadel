package io.citadel.api.event;

/**
 * Dispatches events between core subsystems and plugins.
 *
 * <p>The event bus is the backbone of internal communication in Citadel. Subsystems and plugins
 * communicate by publishing and subscribing to typed events rather than calling each other
 * directly.
 *
 * <p>Two delivery modes are available:
 *
 * <dl>
 *   <dt>Synchronous ({@link #publish(Event)})
 *   <dd>The publisher blocks until all handlers have completed. Use this when the publisher must
 *       wait for handlers (e.g., packet modification events where handlers may modify the event
 *       before it is sent).
 *   <dt>Asynchronous ({@link #publishAsync(Event)})
 *   <dd>The publisher returns immediately and handlers run on a separate thread pool. This is the
 *       preferred mode for most events since it prevents slow handlers from blocking the publisher.
 * </dl>
 *
 * <p>Error isolation: if a handler throws an exception during event processing, the exception is
 * caught and logged. Other handlers subscribed to the same event still receive the event. One
 * failing handler does not affect others.
 *
 * <p>Thread safety: implementations of this interface must be thread-safe. Handlers may be invoked
 * from any thread.
 */
public interface EventBus {

  /**
   * Registers a handler for a specific event type.
   *
   * <p>The handler receives all events of the specified type (and subtypes, depending on the
   * implementation). If the same handler is registered twice for the same event type, the second
   * registration is ignored.
   *
   * @param <T> the event type
   * @param type the event class to subscribe to (not null)
   * @param handler the handler to invoke when an event is published (not null)
   * @throws NullPointerException if type or handler is null
   */
  <T extends Event> void subscribe(Class<T> type, EventHandler<T> handler);

  /**
   * Removes a previously registered handler.
   *
   * <p>If the handler was not registered, this method does nothing.
   *
   * @param <T> the event type
   * @param type the event class the handler was registered for (not null)
   * @param handler the handler to remove (not null)
   * @throws NullPointerException if type or handler is null
   */
  <T extends Event> void unsubscribe(Class<T> type, EventHandler<T> handler);

  /**
   * Publishes an event synchronously.
   *
   * <p>This method blocks until all subscribed handlers have completed. Use {@link
   * #publishAsync(Event)} for events where the publisher does not need to wait for handler
   * completion.
   *
   * @param event the event to publish (not null)
   * @throws NullPointerException if event is null
   */
  void publish(Event event);

  /**
   * Publishes an event asynchronously.
   *
   * <p>This method returns immediately. Handlers are invoked on a separate thread pool. Exceptions
   * thrown by handlers are caught and logged. If you need to wait for handler completion, use
   * {@link #publish(Event)} instead.
   *
   * @param event the event to publish (not null)
   * @throws NullPointerException if event is null
   */
  void publishAsync(Event event);
}
