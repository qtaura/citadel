package io.citadel.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.service.Logger;
import io.citadel.core.event.EventBusImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginSubscriptionTrackerTest {

  private EventBusImpl eventBus;
  private PluginSubscriptionTracker tracker;
  private EventBus pluginBus;

  @BeforeEach
  void setUp() {
    eventBus = new EventBusImpl(new TestLogger());
    tracker = new PluginSubscriptionTracker(eventBus);
    pluginBus = tracker.forPlugin("TestPlugin");
  }

  @AfterEach
  void tearDown() {
    eventBus.shutdown();
  }

  // --- Automatic cleanup ---

  @Test
  void cancelAllRemovesSubscription() {
    List<Event> received = new ArrayList<>();
    pluginBus.subscribe(TestEvent.class, received::add);

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size());

    tracker.cancelAll("TestPlugin");
    eventBus.publish(new TestEvent());
    assertEquals(1, received.size(), "handler should not receive after cancelAll");
  }

  // --- Manual unsubscribe + automatic cleanup ---

  @Test
  void manualUnsubscribeThenCancelAllIsIdempotent() {
    List<Event> received = new ArrayList<>();
    EventHandler<TestEvent> handler = received::add;
    pluginBus.subscribe(TestEvent.class, handler);

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size());

    pluginBus.unsubscribe(TestEvent.class, handler);

    tracker.cancelAll("TestPlugin");

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size(), "handler should not receive after manual unsubscribe");
  }

  // --- Multiple subscriptions ---

  @Test
  void cancelAllCancelsMultipleSubscriptions() {
    AtomicInteger count = new AtomicInteger(0);
    pluginBus.subscribe(TestEvent.class, e -> count.incrementAndGet());
    pluginBus.subscribe(OtherEvent.class, e -> count.incrementAndGet());
    pluginBus.subscribe(Event.class, e -> count.incrementAndGet());

    eventBus.publish(new TestEvent());
    eventBus.publish(new OtherEvent());
    assertEquals(4, count.get());

    tracker.cancelAll("TestPlugin");

    eventBus.publish(new TestEvent());
    eventBus.publish(new OtherEvent());
    assertEquals(4, count.get(), "no handlers should fire after cancelAll");
  }

  // --- Multiple plugins isolation ---

  @Test
  void cancelAllForOnePluginDoesNotAffectAnother() {
    List<Event> pluginA = new ArrayList<>();
    List<Event> pluginB = new ArrayList<>();

    EventBus busA = tracker.forPlugin("PluginA");
    EventBus busB = tracker.forPlugin("PluginB");

    busA.subscribe(TestEvent.class, pluginA::add);
    busB.subscribe(TestEvent.class, pluginB::add);

    tracker.cancelAll("PluginA");

    eventBus.publish(new TestEvent());

    assertEquals(0, pluginA.size(), "PluginA handler should be cancelled");
    assertEquals(1, pluginB.size(), "PluginB handler should still fire");
  }

  // --- No subscriptions ---

  @Test
  void cancelAllWithNoSubscriptionsDoesNothing() {
    tracker.cancelAll("TestPlugin");
  }

  @Test
  void cancelAllForUnknownPluginDoesNothing() {
    tracker.cancelAll("NonExistentPlugin");
  }

  // --- Idempotent ---

  @Test
  void cancelAllIsIdempotent() {
    List<Event> received = new ArrayList<>();
    pluginBus.subscribe(TestEvent.class, received::add);

    tracker.cancelAll("TestPlugin");
    tracker.cancelAll("TestPlugin");
    tracker.cancelAll("TestPlugin");

    eventBus.publish(new TestEvent());
    assertEquals(0, received.size());
  }

  // --- Tracked bus still publishes ---

  @Test
  void trackedBusPublishWorks() {
    List<Event> received = new ArrayList<>();
    eventBus.subscribe(TestEvent.class, received::add);

    pluginBus.publish(new TestEvent());

    assertEquals(1, received.size());
  }

  @Test
  void trackedBusPublishAsyncWorks() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    eventBus.subscribe(TestEvent.class, e -> latch.countDown());

    pluginBus.publishAsync(new TestEvent());

    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

  // --- Tracked bus still allows unsubscribe ---

  @Test
  void trackedBusUnsubscribeWorks() {
    List<Event> received = new ArrayList<>();
    EventHandler<TestEvent> handler = received::add;
    pluginBus.subscribe(TestEvent.class, handler);

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size());

    pluginBus.unsubscribe(TestEvent.class, handler);

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size());
  }

  // --- Concurrent publish during shutdown ---

  @Test
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  void concurrentPublishDuringCancelAllIsSafe() throws Exception {
    AtomicInteger received = new AtomicInteger(0);
    pluginBus.subscribe(TestEvent.class, e -> received.incrementAndGet());

    CountDownLatch publishingDone = new CountDownLatch(3);
    Runnable publisher =
        () -> {
          for (int i = 0; i < 500; i++) {
            eventBus.publish(new TestEvent());
          }
          publishingDone.countDown();
        };

    new Thread(publisher).start();
    new Thread(publisher).start();
    new Thread(publisher).start();

    // Cancel while publishing is happening
    Thread.sleep(50);
    tracker.cancelAll("TestPlugin");

    assertTrue(publishingDone.await(10, TimeUnit.SECONDS));
    assertTrue(received.get() > 0, "some events should have been received before cancel");
  }

  // --- Tracked bus returns real subscriptions ---

  @Test
  void subscriptionFromTrackedBusSupportsCancel() {
    List<Event> received = new ArrayList<>();
    Subscription sub = pluginBus.subscribe(TestEvent.class, received::add);

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size());

    sub.cancel();

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size(), "sub.cancel() should prevent delivery");
  }

  @Test
  void subscriptionFromTrackedBusCanBeCancelledThenTrackerCancelsAgain() {
    List<Event> received = new ArrayList<>();
    Subscription sub = pluginBus.subscribe(TestEvent.class, received::add);

    sub.cancel();
    tracker.cancelAll("TestPlugin");

    eventBus.publish(new TestEvent());
    assertEquals(0, received.size());
  }

  // --- Test event types ---

  private static final class TestEvent extends Event {}

  private static final class OtherEvent extends Event {}

  // --- Test logger ---

  private static final class TestLogger implements Logger {
    @Override
    public boolean isTraceEnabled() {
      return false;
    }

    @Override
    public boolean isDebugEnabled() {
      return false;
    }

    @Override
    public boolean isInfoEnabled() {
      return true;
    }

    @Override
    public boolean isWarnEnabled() {
      return true;
    }

    @Override
    public boolean isErrorEnabled() {
      return true;
    }

    @Override
    public void trace(String message) {}

    @Override
    public void trace(String format, Object... args) {}

    @Override
    public void trace(String message, Throwable throwable) {}

    @Override
    public void trace(String accountName, String message) {}

    @Override
    public void trace(String accountName, String format, Object... args) {}

    @Override
    public void trace(String accountName, String message, Throwable throwable) {}

    @Override
    public void debug(String message) {}

    @Override
    public void debug(String format, Object... args) {}

    @Override
    public void debug(String message, Throwable throwable) {}

    @Override
    public void debug(String accountName, String message) {}

    @Override
    public void debug(String accountName, String format, Object... args) {}

    @Override
    public void debug(String accountName, String message, Throwable throwable) {}

    @Override
    public void info(String message) {}

    @Override
    public void info(String format, Object... args) {}

    @Override
    public void info(String message, Throwable throwable) {}

    @Override
    public void info(String accountName, String message) {}

    @Override
    public void info(String accountName, String format, Object... args) {}

    @Override
    public void info(String accountName, String message, Throwable throwable) {}

    @Override
    public void warn(String message) {}

    @Override
    public void warn(String format, Object... args) {}

    @Override
    public void warn(String message, Throwable throwable) {}

    @Override
    public void warn(String accountName, String message) {}

    @Override
    public void warn(String accountName, String format, Object... args) {}

    @Override
    public void warn(String accountName, String message, Throwable throwable) {}

    @Override
    public void error(String message) {}

    @Override
    public void error(String format, Object... args) {}

    @Override
    public void error(String message, Throwable throwable) {}

    @Override
    public void error(String accountName, String message) {}

    @Override
    public void error(String accountName, String format, Object... args) {}

    @Override
    public void error(String accountName, String message, Throwable throwable) {}
  }
}
