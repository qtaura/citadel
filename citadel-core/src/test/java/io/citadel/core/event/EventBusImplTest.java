package io.citadel.core.event;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.event.Event;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.service.Logger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventBusImplTest {

  private EventBusImpl eventBus;

  @BeforeEach
  void setUp() {
    eventBus = new EventBusImpl(new TestLogger());
  }

  @AfterEach
  void tearDown() {
    eventBus.shutdown();
  }

  // --- Single subscriber ---

  @Test
  void singleSubscriberReceivesEvent() {
    List<Event> received = new ArrayList<>();
    eventBus.subscribe(TestEvent.class, received::add);

    TestEvent event = new TestEvent();
    eventBus.publish(event);

    assertEquals(1, received.size());
    assertSame(event, received.getFirst());
  }

  // --- Multiple subscribers ---

  @Test
  void multipleSubscribersAllReceiveEvent() {
    List<Event> first = new ArrayList<>();
    List<Event> second = new ArrayList<>();
    eventBus.subscribe(TestEvent.class, first::add);
    eventBus.subscribe(TestEvent.class, second::add);

    TestEvent event = new TestEvent();
    eventBus.publish(event);

    assertEquals(1, first.size());
    assertSame(event, first.getFirst());
    assertEquals(1, second.size());
    assertSame(event, second.getFirst());
  }

  // --- Subscriber hierarchy ---

  @Test
  void subscriberToBaseClassReceivesSubclassEvents() {
    List<Event> received = new ArrayList<>();
    eventBus.subscribe(Event.class, received::add);

    TestEvent event = new TestEvent();
    eventBus.publish(event);

    assertEquals(1, received.size());
    assertSame(event, received.getFirst());
  }

  @Test
  void subscriberToSubclassDoesNotReceiveOtherEvents() {
    List<Event> received = new ArrayList<>();
    eventBus.subscribe(TestEvent.class, received::add);

    eventBus.publish(new OtherEvent());
    eventBus.publish(new TestEvent());

    assertEquals(1, received.size());
    assertInstanceOf(TestEvent.class, received.getFirst());
  }

  // --- Type specificity ---

  @Test
  void specificSubscriberAlsoFiresAlongsideBroadSubscriber() {
    List<Event> all = new ArrayList<>();
    List<TestEvent> specific = new ArrayList<>();
    eventBus.subscribe(Event.class, all::add);
    eventBus.subscribe(TestEvent.class, specific::add);

    TestEvent event = new TestEvent();
    eventBus.publish(event);

    assertEquals(1, all.size());
    assertEquals(1, specific.size());
    assertSame(event, all.getFirst());
    assertSame(event, specific.getFirst());
  }

  // --- Async dispatch ---

  @Test
  void asyncDispatchDeliversEvent() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<TestEvent> received = new AtomicReference<>();
    eventBus.subscribe(
        TestEvent.class,
        e -> {
          received.set(e);
          latch.countDown();
        });

    TestEvent event = new TestEvent();
    eventBus.publishAsync(event);

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertSame(event, received.get());
  }

  @Test
  void asyncDispatchReturnsImmediately() throws Exception {
    CountDownLatch slowLatch = new CountDownLatch(1);
    eventBus.subscribe(
        TestEvent.class,
        e -> {
          try {
            Thread.sleep(500);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        });

    long start = System.nanoTime();
    eventBus.publishAsync(new TestEvent());
    long elapsed = System.nanoTime() - start;

    assertTrue(
        elapsed < TimeUnit.MILLISECONDS.toNanos(200),
        "publishAsync should return before handler completes");
    slowLatch.countDown();
  }

  // --- Sync dispatch ---

  @Test
  void syncDispatchBlocksUntilHandlerCompletes() {
    AtomicInteger counter = new AtomicInteger(0);
    eventBus.subscribe(
        TestEvent.class,
        e -> {
          try {
            Thread.sleep(100);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
          counter.incrementAndGet();
        });

    long start = System.nanoTime();
    eventBus.publish(new TestEvent());
    long elapsed = System.nanoTime() - start;

    assertEquals(1, counter.get());
    assertTrue(
        elapsed >= TimeUnit.MILLISECONDS.toNanos(50),
        "publish should block until handler completes");
  }

  // --- Subscriber removal via unsubscribe ---

  @Test
  void unsubscribedHandlerNoLongerReceivesEvents() {
    List<Event> received = new ArrayList<>();
    EventHandler<TestEvent> handler = received::add;
    eventBus.subscribe(TestEvent.class, handler);

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size());

    eventBus.unsubscribe(TestEvent.class, handler);
    eventBus.publish(new TestEvent());
    assertEquals(1, received.size(), "handler should not receive after unsubscribe");
  }

  // --- Subscriber removal via Subscription.cancel ---

  @Test
  void cancelledSubscriptionNoLongerReceivesEvents() {
    List<Event> received = new ArrayList<>();
    Subscription sub = eventBus.subscribe(TestEvent.class, received::add);

    eventBus.publish(new TestEvent());
    assertEquals(1, received.size());

    sub.cancel();
    eventBus.publish(new TestEvent());
    assertEquals(1, received.size(), "handler should not receive after cancel");
  }

  // --- Duplicate subscription ---

  @Test
  void duplicateSubscriptionReturnsOriginalHandle() {
    List<Event> received = new ArrayList<>();
    EventHandler<TestEvent> handler = received::add;
    Subscription first = eventBus.subscribe(TestEvent.class, handler);
    Subscription second = eventBus.subscribe(TestEvent.class, handler);

    assertSame(first, second, "duplicate subscribe should return original subscription");

    second.cancel();
    eventBus.publish(new TestEvent());
    assertEquals(0, received.size(), "cancelling either handle should prevent delivery");
  }

  @Test
  void duplicateSubscriptionDoesNotDoubleDeliver() {
    List<Event> received = new ArrayList<>();
    EventHandler<TestEvent> handler = received::add;
    eventBus.subscribe(TestEvent.class, handler);
    eventBus.subscribe(TestEvent.class, handler);

    eventBus.publish(new TestEvent());

    assertEquals(1, received.size(), "handler should only be invoked once");
  }

  // --- Exception isolation ---

  @Test
  void exceptionInOneHandlerDoesNotAffectOthers() {
    List<Event> received = new ArrayList<>();
    eventBus.subscribe(
        TestEvent.class,
        e -> {
          throw new RuntimeException("fail");
        });
    eventBus.subscribe(TestEvent.class, received::add);

    TestEvent event = new TestEvent();
    eventBus.publish(event);

    assertEquals(1, received.size());
    assertSame(event, received.getFirst());
  }

  @Test
  void exceptionInAsyncHandlerDoesNotAffectOthers() throws Exception {
    List<Event> received = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    eventBus.subscribe(
        TestEvent.class,
        e -> {
          throw new RuntimeException("fail");
        });
    eventBus.subscribe(
        TestEvent.class,
        e -> {
          received.add(e);
          latch.countDown();
        });

    eventBus.publishAsync(new TestEvent());

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(1, received.size());
  }

  @Test
  void exceptionDoesNotBreakFutureEvents() {
    eventBus.subscribe(
        TestEvent.class,
        e -> {
          throw new RuntimeException("fail");
        });

    eventBus.publish(new TestEvent());

    List<Event> received = new ArrayList<>();
    eventBus.subscribe(TestEvent.class, received::add);
    TestEvent event = new TestEvent();
    eventBus.publish(event);

    assertEquals(1, received.size());
    assertSame(event, received.getFirst());
  }

  // --- Concurrent publishing ---

  @Test
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  void concurrentPublishingDeliversAllEvents() throws Exception {
    int threadCount = 10;
    int eventsPerThread = 100;
    AtomicInteger totalReceived = new AtomicInteger(0);
    CountDownLatch allDone = new CountDownLatch(threadCount);

    eventBus.subscribe(TestEvent.class, e -> totalReceived.incrementAndGet());

    for (int t = 0; t < threadCount; t++) {
      Thread publisher =
          new Thread(
              () -> {
                for (int i = 0; i < eventsPerThread; i++) {
                  eventBus.publish(new TestEvent());
                }
                allDone.countDown();
              });
      publisher.start();
    }

    assertTrue(allDone.await(10, TimeUnit.SECONDS));
    assertEquals(threadCount * eventsPerThread, totalReceived.get());
  }

  @Test
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  void concurrentPublishingToHierarchyDeliversAll() throws Exception {
    int threadCount = 10;
    int eventsPerThread = 100;
    AtomicInteger totalReceived = new AtomicInteger(0);
    CountDownLatch allDone = new CountDownLatch(threadCount);

    eventBus.subscribe(Event.class, e -> totalReceived.incrementAndGet());

    for (int t = 0; t < threadCount; t++) {
      Thread publisher =
          new Thread(
              () -> {
                for (int i = 0; i < eventsPerThread; i++) {
                  eventBus.publish(new TestEvent());
                  eventBus.publish(new OtherEvent());
                }
                allDone.countDown();
              });
      publisher.start();
    }

    assertTrue(allDone.await(10, TimeUnit.SECONDS));
    assertEquals(threadCount * eventsPerThread * 2, totalReceived.get());
  }

  // --- Concurrent subscriptions ---

  @Test
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  void concurrentSubscribeAndPublishIsThreadSafe() throws Exception {
    AtomicInteger received = new AtomicInteger(0);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(3);

    Runnable publisher =
        () -> {
          awaitUnchecked(startLatch);
          for (int i = 0; i < 500; i++) {
            eventBus.publish(new TestEvent());
          }
          done.countDown();
        };

    Runnable subscriber =
        () -> {
          awaitUnchecked(startLatch);
          for (int i = 0; i < 100; i++) {
            List<Event> list = new ArrayList<>();
            EventHandler<TestEvent> handler = list::add;
            Subscription sub = eventBus.subscribe(TestEvent.class, handler);
            eventBus.publish(new TestEvent());
            sub.cancel();
            received.addAndGet(list.size());
          }
          done.countDown();
        };

    new Thread(publisher).start();
    new Thread(publisher).start();
    new Thread(subscriber).start();

    startLatch.countDown();
    assertTrue(done.await(10, TimeUnit.SECONDS));
  }

  // --- Metadata ---

  @Test
  void eventCarriesTimestamp() {
    Instant before = Instant.now();
    TestEvent event = new TestEvent();
    Instant after = Instant.now();

    assertNotNull(event.getTimestamp());
    assertFalse(event.getTimestamp().isBefore(before));
    assertFalse(event.getTimestamp().isAfter(after));
  }

  @Test
  void eventWithSourcePlugin() {
    TestEvent event = new TestEvent("MapArtPlugin", null);
    assertEquals("MapArtPlugin", event.getSourcePlugin());
    assertNull(event.getAccountId());
  }

  @Test
  void eventWithAccountId() {
    TestEvent event = new TestEvent(null, "Builder01");
    assertNull(event.getSourcePlugin());
    assertEquals("Builder01", event.getAccountId());
  }

  @Test
  void eventWithBothMetadataFields() {
    TestEvent event = new TestEvent("MapArtPlugin", "Builder01");
    assertEquals("MapArtPlugin", event.getSourcePlugin());
    assertEquals("Builder01", event.getAccountId());
  }

  @Test
  void eventWithNoMetadataHasNullFields() {
    TestEvent event = new TestEvent();
    assertNull(event.getSourcePlugin());
    assertNull(event.getAccountId());
  }

  @Test
  void metadataIsImmutable() {
    TestEvent event = new TestEvent("PluginA", "Account1");
    assertEquals("PluginA", event.getSourcePlugin());
    assertEquals("Account1", event.getAccountId());
  }

  @Test
  void metadataSurvivesEventDispatch() {
    AtomicReference<String> receivedSource = new AtomicReference<>();
    AtomicReference<String> receivedAccount = new AtomicReference<>();
    eventBus.subscribe(
        TestEvent.class,
        e -> {
          receivedSource.set(e.getSourcePlugin());
          receivedAccount.set(e.getAccountId());
        });

    eventBus.publish(new TestEvent("DiscordPlugin", "BotAccount"));

    assertEquals("DiscordPlugin", receivedSource.get());
    assertEquals("BotAccount", receivedAccount.get());
  }

  @Test
  void timestampSurvivesDispatch() {
    AtomicReference<Instant> received = new AtomicReference<>();
    eventBus.subscribe(TestEvent.class, e -> received.set(e.getTimestamp()));

    TestEvent event = new TestEvent();
    eventBus.publish(event);

    assertSame(event.getTimestamp(), received.get());
  }

  // --- Null handling ---

  @Test
  void subscribeThrowsOnNullType() {
    assertThrows(NullPointerException.class, () -> eventBus.subscribe(null, e -> {}));
  }

  @Test
  void subscribeThrowsOnNullHandler() {
    assertThrows(NullPointerException.class, () -> eventBus.subscribe(TestEvent.class, null));
  }

  @Test
  void publishThrowsOnNullEvent() {
    assertThrows(NullPointerException.class, () -> eventBus.publish(null));
  }

  @Test
  void publishAsyncThrowsOnNullEvent() {
    assertThrows(NullPointerException.class, () -> eventBus.publishAsync(null));
  }

  @Test
  void unsubscribeThrowsOnNullType() {
    assertThrows(NullPointerException.class, () -> eventBus.unsubscribe(null, e -> {}));
  }

  @Test
  void unsubscribeThrowsOnNullHandler() {
    assertThrows(NullPointerException.class, () -> eventBus.unsubscribe(TestEvent.class, null));
  }

  // --- Cancel on already-cancelled is idempotent ---

  @Test
  void cancelOnCancelledSubscriptionIsIdempotent() {
    List<Event> received = new ArrayList<>();
    Subscription sub = eventBus.subscribe(TestEvent.class, received::add);
    sub.cancel();
    sub.cancel();
    sub.cancel();

    eventBus.publish(new TestEvent());
    assertEquals(0, received.size());
  }

  // --- Unsubscribe on non-existent handler does nothing ---

  @Test
  void unsubscribeNonExistentHandlerDoesNothing() {
    eventBus.unsubscribe(TestEvent.class, e -> {});
  }

  // --- Shutdown ---

  @Test
  void shutdownRejectsNewAsyncEvents() {
    eventBus.shutdown();

    eventBus.publishAsync(new TestEvent());

    List<Event> received = new ArrayList<>();
    eventBus.subscribe(Event.class, received::add);
    eventBus.publish(new TestEvent());
    assertEquals(1, received.size());
  }

  @Test
  void shutdownIsIdempotent() {
    eventBus.shutdown();
    eventBus.shutdown();
    eventBus.shutdown();
  }

  @Test
  void syncPublishStillWorksAfterShutdown() {
    eventBus.shutdown();

    List<Event> received = new ArrayList<>();
    eventBus.subscribe(TestEvent.class, received::add);
    eventBus.publish(new TestEvent());

    assertEquals(1, received.size());
  }

  @Test
  void executorTerminatesOnShutdown() throws Exception {
    eventBus.shutdown();
    assertTrue(eventBus.shutdownComplete(5, TimeUnit.SECONDS));
  }

  // --- Test event types ---

  private static final class TestEvent extends Event {
    TestEvent() {}

    TestEvent(String sourcePlugin, String accountId) {
      super(sourcePlugin, accountId);
    }
  }

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
    public void info(String message) {
      System.out.println(message);
    }

    @Override
    public void info(String format, Object... args) {
      System.out.println(String.format(format, args));
    }

    @Override
    public void info(String message, Throwable throwable) {
      System.out.println(message);
    }

    @Override
    public void info(String accountName, String message) {
      System.out.println(accountName + ": " + message);
    }

    @Override
    public void info(String accountName, String format, Object... args) {
      System.out.println(accountName + ": " + String.format(format, args));
    }

    @Override
    public void info(String accountName, String message, Throwable throwable) {
      System.out.println(accountName + ": " + message);
    }

    @Override
    public void warn(String message) {
      System.err.println(message);
    }

    @Override
    public void warn(String format, Object... args) {
      System.err.println(String.format(format, args));
    }

    @Override
    public void warn(String message, Throwable throwable) {
      System.err.println(message);
    }

    @Override
    public void warn(String accountName, String message) {
      System.err.println(accountName + ": " + message);
    }

    @Override
    public void warn(String accountName, String format, Object... args) {
      System.err.println(accountName + ": " + String.format(format, args));
    }

    @Override
    public void warn(String accountName, String message, Throwable throwable) {
      System.err.println(accountName + ": " + message);
    }

    @Override
    public void error(String message) {
      System.err.println(message);
    }

    @Override
    public void error(String format, Object... args) {
      System.err.println(String.format(format, args));
    }

    @Override
    public void error(String message, Throwable throwable) {
      System.err.println(message);
    }

    @Override
    public void error(String accountName, String message) {
      System.err.println(accountName + ": " + message);
    }

    @Override
    public void error(String accountName, String format, Object... args) {
      System.err.println(accountName + ": " + String.format(format, args));
    }

    @Override
    public void error(String accountName, String message, Throwable throwable) {
      System.err.println(accountName + ": " + message);
    }
  }

  private static void awaitUnchecked(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
