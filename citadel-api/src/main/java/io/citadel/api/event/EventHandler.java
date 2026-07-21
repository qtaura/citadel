package io.citadel.api.event;

/**
 * Receives and processes events published through the {@link EventBus}.
 *
 * <p>This is a functional interface whose single method is invoked when an event of the subscribed
 * type is published. Handlers should be stateless where possible, or at minimum thread-safe, since
 * they may be invoked from multiple threads.
 *
 * <p>If a handler throws an exception, the exception is caught and logged by the event bus. Other
 * handlers subscribed to the same event type will still receive the event. A handler that
 * consistently throws may be removed from the subscription list by the implementation.
 *
 * @param <T> the event type this handler accepts
 */
@FunctionalInterface
public interface EventHandler<T extends Event> {

  /**
   * Called when an event of the subscribed type is published.
   *
   * <p>Implementations should not block for extended periods. Long-running work should be offloaded
   * using the {@link io.citadel.api.service.Scheduler} service.
   *
   * @param event the event instance (never null)
   */
  void handle(T event);
}
