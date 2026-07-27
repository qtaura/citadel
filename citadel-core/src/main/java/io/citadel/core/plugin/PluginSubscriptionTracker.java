package io.citadel.core.plugin;

import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PluginSubscriptionTracker {

  private final EventBus eventBus;
  private final Map<String, List<TrackedSubscription>> subscriptions;

  public PluginSubscriptionTracker(EventBus eventBus) {
    this.eventBus = eventBus;
    this.subscriptions = new ConcurrentHashMap<>();
  }

  public EventBus forPlugin(String pluginName) {
    return new TrackedEventBus(pluginName);
  }

  public void cancelAll(String pluginName) {
    List<TrackedSubscription> subs = subscriptions.remove(pluginName);
    if (subs != null) {
      for (TrackedSubscription sub : subs) {
        sub.delegate.cancel();
      }
    }
  }

  private static final class TrackedSubscription {
    final Class<?> type;
    final EventHandler<?> handler;
    final Subscription delegate;

    TrackedSubscription(Class<?> type, EventHandler<?> handler, Subscription delegate) {
      this.type = type;
      this.handler = handler;
      this.delegate = delegate;
    }
  }

  private class TrackedEventBus implements EventBus {

    private final String pluginName;

    TrackedEventBus(String pluginName) {
      this.pluginName = pluginName;
    }

    @Override
    public <T extends Event> Subscription subscribe(Class<T> type, EventHandler<T> handler) {
      Subscription sub = eventBus.subscribe(type, handler);
      TrackedSubscription tracked = new TrackedSubscription(type, handler, sub);
      subscriptions.computeIfAbsent(pluginName, k -> new CopyOnWriteArrayList<>()).add(tracked);
      return sub;
    }

    @Override
    public <T extends Event> void unsubscribe(Class<T> type, EventHandler<T> handler) {
      eventBus.unsubscribe(type, handler);
      List<TrackedSubscription> subs = subscriptions.get(pluginName);
      if (subs != null) {
        subs.removeIf(ts -> ts.type.equals(type) && ts.handler.equals(handler));
      }
    }

    @Override
    public void publish(Event event) {
      eventBus.publish(event);
    }

    @Override
    public void publishAsync(Event event) {
      eventBus.publishAsync(event);
    }
  }
}
