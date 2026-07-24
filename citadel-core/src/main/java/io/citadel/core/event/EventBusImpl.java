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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EventBusImpl implements EventBus {

  private final Map<Class<?>, List<HandlerEntry<?>>> handlers;
  private final ExecutorService executor;
  private final Logger logger;
  private volatile boolean isShutDown;

  private static final class HandlerEntry<T extends Event> {
    final EventHandler<T> handler;
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    final Subscription subscription;

    HandlerEntry(EventHandler<T> handler) {
      this.handler = handler;
      this.subscription = () -> cancelled.set(true);
    }
  }

  public EventBusImpl(Logger logger) {
    this.handlers = new ConcurrentHashMap<>();
    this.executor = Executors.newCachedThreadPool(r -> new Thread(r, "event-bus"));
    this.logger = logger;
  }

  @Override
  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  public <T extends Event> Subscription subscribe(Class<T> type, EventHandler<T> handler) {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(handler, "handler must not be null");
    HandlerEntry<T> entry = new HandlerEntry<>(handler);
    @SuppressWarnings("PMD.LooseCoupling")
    CopyOnWriteArrayList<HandlerEntry<?>> list =
        (CopyOnWriteArrayList<HandlerEntry<?>>)
            handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());

    synchronized (list) {
      for (HandlerEntry<?> existing : list) {
        if (existing.handler == handler) {
          return existing.subscription;
        }
      }
      list.add(entry);
    }

    return entry.subscription;
  }

  @Override
  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  public <T extends Event> void unsubscribe(Class<T> type, EventHandler<T> handler) {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(handler, "handler must not be null");
    List<HandlerEntry<?>> list = handlers.get(type);
    if (list != null) {
      list.removeIf(e -> e.handler == handler);
    }
  }

  @Override
  public void publish(Event event) {
    Objects.requireNonNull(event, "event must not be null");
    dispatch(event, false);
  }

  @Override
  public void publishAsync(Event event) {
    Objects.requireNonNull(event, "event must not be null");
    if (isShutDown) {
      return;
    }
    executor.submit(() -> dispatch(event, true));
  }

  public void shutdown() {
    isShutDown = true;
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  boolean shutdownComplete(long timeout, TimeUnit unit) throws InterruptedException {
    return executor.awaitTermination(0, unit);
  }

  private void dispatch(Event event, boolean isAsync) {
    Class<?> type = event.getClass();
    while (type != null && Event.class.isAssignableFrom(type)) {
      List<HandlerEntry<?>> list = handlers.get(type);
      if (list != null) {
        for (HandlerEntry<?> entry : list) {
          if (!entry.cancelled.get()) {
            invokeHandler(entry, event, isAsync);
          }
        }
      }
      type = type.getSuperclass();
    }
  }

  @SuppressWarnings("unchecked")
  private void invokeHandler(HandlerEntry<?> entry, Event event, boolean isAsync) {
    try {
      ((EventHandler<Event>) entry.handler).handle(event);
    } catch (Exception e) {
      logger.error(
          "Event handler threw exception processing {} (async={}): {}",
          event.getClass().getSimpleName(),
          isAsync,
          e.getMessage());
    }
  }
}
