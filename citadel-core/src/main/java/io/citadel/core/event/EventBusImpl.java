package io.citadel.core.event;

import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.service.Logger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EventBusImpl implements EventBus {

  private final Logger logger;
  private final Map<Class<?>, List<HandlerEntry>> handlers;
  private final ExecutorService asyncExecutor;
  private final AtomicBoolean shutdown;

  public EventBusImpl(Logger logger) {
    this.logger = logger;
    this.handlers = new ConcurrentHashMap<>();
    this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    this.shutdown = new AtomicBoolean(false);
  }

  @Override
  public <T extends Event> Subscription subscribe(Class<T> type, EventHandler<T> handler) {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(handler, "handler must not be null");

    List<HandlerEntry> entries =
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());

    for (HandlerEntry entry : entries) {
      if (!entry.cancelled.get() && entry.handler == handler) {
        return entry.subscription;
      }
    }

    var entry = new HandlerEntry(handler);
    entries.add(entry);
    entry.subscription =
        () -> {
          if (entry.cancelled.compareAndSet(false, true)) {
            entries.remove(entry);
          }
        };
    return entry.subscription;
  }

  @Override
  public <T extends Event> void unsubscribe(Class<T> type, EventHandler<T> handler) {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(handler, "handler must not be null");

    List<HandlerEntry> entries = handlers.get(type);
    if (entries != null) {
      for (HandlerEntry entry : entries) {
        if (entry.handler == handler) {
          entry.cancelled.set(true);
        }
      }
      entries.removeIf(e -> e.cancelled.get());
    }
  }

  @Override
  public void publish(Event event) {
    Objects.requireNonNull(event, "event must not be null");
    dispatch(event);
  }

  @Override
  public void publishAsync(Event event) {
    Objects.requireNonNull(event, "event must not be null");
    if (shutdown.get()) {
      return;
    }
    try {
      asyncExecutor.execute(() -> dispatch(event));
    } catch (RejectedExecutionException e) {
      // Executor was shut down between the check and execute
    }
  }

  private void dispatch(Event event) {
    Class<?> type = event.getClass();
    while (type != null && type != Object.class && Event.class.isAssignableFrom(type)) {
      List<HandlerEntry> entries = handlers.get(type);
      if (entries != null) {
        for (HandlerEntry entry : entries) {
          if (!entry.cancelled.get()) {
            try {
              @SuppressWarnings("unchecked")
              EventHandler<Event> handler = (EventHandler<Event>) entry.handler;
              handler.handle(event);
            } catch (Exception e) {
              logger.error(
                  "Event handler threw exception for event type {}",
                  event.getClass().getName());
            }
          }
        }
      }
      type = type.getSuperclass();
    }
  }

  public void shutdown() {
    shutdown.set(true);
    asyncExecutor.shutdown();
  }

  boolean shutdownComplete(long timeout, TimeUnit unit) {
    try {
      return asyncExecutor.awaitTermination(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  boolean isShutdown() {
    return shutdown.get();
  }

  static final class HandlerEntry {
    final EventHandler<?> handler;
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    Subscription subscription;

    HandlerEntry(EventHandler<?> handler) {
      this.handler = handler;
    }
  }
}
