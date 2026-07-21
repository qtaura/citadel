package io.citadel.api.event;

/**
 * A handle for a registered event subscription.
 *
 * <p>Returned by {@link EventBus#subscribe(Class, EventHandler)} to allow callers to unsubscribe
 * without retaining a reference to the original handler. This is especially important when using
 * lambda expressions as handlers, since each lambda invocation creates a distinct object instance.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Subscription sub = eventBus.subscribe(MyEvent.class, this::onMyEvent);
 * // later:
 * sub.cancel();
 * }</pre>
 *
 * <p>Calling {@link #cancel()} on an already-cancelled subscription has no effect.
 */
public interface Subscription {

  /**
   * Cancels this subscription.
   *
   * <p>After calling this method, the associated handler will no longer receive events. Calling
   * this method more than once has no effect.
   */
  void cancel();
}
