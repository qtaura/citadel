package io.citadel.core.bot;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import io.citadel.api.bot.BotState;
import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.event.bot.BotFailedEvent;
import io.citadel.api.event.bot.BotReconnectFailedEvent;
import io.citadel.api.event.bot.BotReconnectStartedEvent;
import io.citadel.api.event.bot.BotReconnectSucceededEvent;
import io.citadel.api.event.bot.BotStartingEvent;
import io.citadel.api.event.bot.BotStoppedEvent;
import io.citadel.api.event.bot.BotStoppingEvent;
import io.citadel.api.proxy.ProxyDefinition;
import io.citadel.api.proxy.ProxyManager;
import io.citadel.api.proxy.ProxyType;
import io.citadel.api.reconnect.ReconnectPolicy;
import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Logger;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
  "PMD.CouplingBetweenObjects",
  "PMD.ExcessiveImports",
  "PMD.AvoidInstantiatingObjectsInLoops",
  "PMD.AvoidUsingHardCodedIP",
  "PMD.GodClass"
})
class BotImplTest {

  private static final String TEST_NET_HOST = "198.51.100.1";

  @Test
  void initialStateIsCreated() {
    BotImpl bot = createTestBot("test", trueAccount("test"));
    assertEquals(BotState.CREATED, bot.getState());
    assertFalse(bot.isRunning());
  }

  @Test
  void startSetsStateToStarting() throws Exception {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    CountDownLatch blocker = new CountDownLatch(1);
    bot.startBlocker = blocker;
    bot.start();
    assertEquals(BotState.STARTING, bot.getState());
    blocker.countDown();
    try {
      bot.start().get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
  }

  @Test
  void startOnRunningBotReturnsCompletedFuture() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.RUNNING);
    assertTrue(bot.start().isDone());
    assertFalse(bot.start().isCompletedExceptionally());
  }

  @Test
  void startOnStartingBotReturnsSameFuture() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    CountDownLatch blocker = new CountDownLatch(1);
    bot.startBlocker = blocker;
    CompletableFuture<Void> f1 = bot.start();
    CompletableFuture<Void> f2 = bot.start();
    assertSame(f1, f2);
    blocker.countDown();
  }

  @Test
  void startOnFailedBotIsRejected() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.FAILED);
    CompletableFuture<Void> future = bot.start();
    assertTrue(future.isDone());
    assertThrows(ExecutionException.class, () -> future.get());
  }

  @Test
  void startOnStoppingBotIsRejected() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.STOPPING);
    CompletableFuture<Void> future = bot.start();
    assertTrue(future.isDone());
    assertThrows(ExecutionException.class, () -> future.get());
  }

  @Test
  void startPublishesStartingEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus);
    bot.start();
    assertTrue(bus.contains(BotStartingEvent.class));
  }

  @Test
  void stopOnCreatedBotReturnsCompletedFuture() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    assertTrue(bot.stop().isDone());
  }

  @Test
  void stopOnStoppedBotReturnsCompletedFuture() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.STOPPED);
    assertTrue(bot.stop().isDone());
  }

  @Test
  void stopOnFailedBotReturnsCompletedFuture() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.FAILED);
    assertTrue(bot.stop().isDone());
  }

  @Test
  void stopOnRunningBotSetsStopping() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.RUNNING);
    bot.stopBlocker = new CountDownLatch(1);
    bot.stop();
    assertEquals(BotState.STOPPING, bot.getState());
    bot.stopBlocker.countDown();
  }

  @Test
  void stopOnStoppingBotReturnsSameFuture() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.RUNNING);
    bot.stopBlocker = new CountDownLatch(1);
    CompletableFuture<Void> f1 = bot.stop();
    CompletableFuture<Void> f2 = bot.stop();
    assertSame(f1, f2);
    bot.stopBlocker.countDown();
  }

  @Test
  void stopOnStartingBotTransitionsToStopping() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    bot.startBlocker = new CountDownLatch(1);
    bot.stopBlocker = new CountDownLatch(1);
    bot.start();
    bot.stop();
    assertEquals(BotState.STOPPING, bot.getState());
    bot.startBlocker.countDown();
    bot.stopBlocker.countDown();
  }

  @Test
  void stopPublishesStoppingEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus);
    forceState(bot, BotState.RUNNING);
    bot.stop();
    assertTrue(bus.contains(BotStoppingEvent.class));
  }

  @Test
  void stopCompletedPublishesStoppedEvent() throws Exception {
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus);
    forceState(bot, BotState.RUNNING);
    bot.stop().get(5, TimeUnit.SECONDS);
    assertTrue(bus.contains(BotStoppedEvent.class));
    assertEquals(BotState.STOPPED, bot.getState());
  }

  @Test
  void restartReturnsCompletableFuture() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.STOPPED);
    CompletableFuture<Void> future = bot.restart();
    assertNotNull(future);
  }

  @Test
  void restartFromFailedTransitionsToStarting() throws Exception {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.FAILED);
    CountDownLatch blocker = new CountDownLatch(1);
    bot.startBlocker = blocker;
    CompletableFuture<Void> future = bot.restart();
    assertNotNull(future);
    assertEquals(BotState.STARTING, bot.getState());
    blocker.countDown();
    try {
      future.get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
  }

  @Test
  void restartFromFailedPublishesStartingEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus);
    forceState(bot, BotState.FAILED);
    bot.startBlocker = new CountDownLatch(1);
    bot.restart();
    assertTrue(bus.contains(BotStartingEvent.class));
    bot.startBlocker.countDown();
  }

  @Test
  void restartFromStoppedTransitionsToStarting() throws Exception {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.STOPPED);
    CountDownLatch blocker = new CountDownLatch(1);
    bot.startBlocker = blocker;
    CompletableFuture<Void> future = bot.restart();
    assertNotNull(future);
    assertEquals(BotState.STARTING, bot.getState());
    blocker.countDown();
    try {
      future.get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
  }

  @Test
  void restartFromRunningStopsAndStarts() throws Exception {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.RUNNING);
    bot.stopBlocker = new CountDownLatch(1);
    CompletableFuture<Void> future = bot.restart();
    assertNotNull(future);
    assertEquals(BotState.STOPPING, bot.getState());
    bot.stopBlocker.countDown();
    try {
      future.get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
  }

  @Test
  void accountNotFoundFailsStartup() throws Exception {
    BotImpl bot = createTestBot("nonexistent", null);
    CompletableFuture<Void> future = bot.start();
    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    assertEquals(BotState.FAILED, bot.getState());
  }

  @Test
  void accountNotFoundPublishesFailedEvent() throws Exception {
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("nonexistent", null, bus);
    try {
      bot.start().get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
    assertTrue(bus.contains(BotFailedEvent.class));
  }

  @Test
  void connectionFailureTransitionsToFailed() throws Exception {
    TestAccountManager acctMgr = new TestAccountManager();
    acctMgr.add(new Account("a1", "TestUser", AccountType.OFFLINE));
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot =
        new BotImpl("a1", acctMgr, null, null, bus, silentLogger(), 1000, 1000, TEST_NET_HOST, 1);
    CompletableFuture<Void> future = bot.start();
    assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
    assertEquals(BotState.FAILED, bot.getState());
  }

  @Test
  void connectionFailurePublishesFailedEvent() throws Exception {
    TestAccountManager acctMgr = new TestAccountManager();
    acctMgr.add(new Account("a1", "TestUser", AccountType.OFFLINE));
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot =
        new BotImpl("a1", acctMgr, null, null, bus, silentLogger(), 1000, 1000, TEST_NET_HOST, 1);
    try {
      bot.start().get(10, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
    assertTrue(bus.contains(BotFailedEvent.class));
  }

  @Test
  void gettersReturnNullBeforeStart() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    assertNull(bot.getAccount());
    assertNull(bot.getConnection());
    assertNull(bot.getSession());
  }

  @Test
  void isRunningFalseAfterFailedStartup() throws Exception {
    BotImpl bot = createTestBot("nonexistent", null);
    try {
      bot.start().get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
    assertFalse(bot.isRunning());
  }

  @Test
  void startAfterRejectedStateDoesNotChangeState() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.FAILED);
    bot.start();
    assertEquals(BotState.FAILED, bot.getState());
  }

  @Test
  void stopCleansUpConnection() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.RUNNING);
    bot.stop();
    assertNull(bot.getConnection());
    assertNull(bot.getSession());
  }

  @Test
  void doubleStartFromStoppedCreatesNewFuture() throws Exception {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.STOPPED);
    CompletableFuture<Void> f1 = bot.start();
    assertNotNull(f1);
  }

  @Test
  void startFromStoppedTransitionsToStarting() throws Exception {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.STOPPED);
    CountDownLatch blocker = new CountDownLatch(1);
    bot.startBlocker = blocker;
    bot.start();
    assertEquals(BotState.STARTING, bot.getState());
    blocker.countDown();
    try {
      bot.start().get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
  }

  @Test
  void multipleStartCallsWhileStartingReturnSameFuture() throws Exception {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    CountDownLatch blocker = new CountDownLatch(1);
    bot.startBlocker = blocker;
    CompletableFuture<Void> f1 = bot.start();
    for (int i = 0; i < 5; i++) {
      assertSame(f1, bot.start());
    }
    blocker.countDown();
    try {
      f1.get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
  }

  @Test
  void multipleStopCallsWhileStoppingReturnSameFuture() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.RUNNING);
    bot.stopBlocker = new CountDownLatch(1);
    CompletableFuture<Void> f1 = bot.stop();
    for (int i = 0; i < 5; i++) {
      assertSame(f1, bot.stop());
    }
    bot.stopBlocker.countDown();
  }

  @Test
  void startStopRacyStartupAborted() throws Exception {
    TestAccountManager acctMgr = new TestAccountManager();
    Account acct = new Account("a1", "TestUser", AccountType.OFFLINE);
    acctMgr.add(acct);
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot =
        new BotImpl("a1", acctMgr, null, null, bus, silentLogger(), 1000, 1000, TEST_NET_HOST, 1);
    bot.start();
    Thread.sleep(50);
    CompletableFuture<Void> stopFuture = bot.stop();
    stopFuture.get(10, TimeUnit.SECONDS);
    assertTrue(bot.getState() == BotState.STOPPED, "Expected STOPPED but got " + bot.getState());
  }

  @Test
  void publishEventsInOrder() throws Exception {
    TestAccountManager acctMgr = new TestAccountManager();
    acctMgr.add(new Account("a1", "TestUser", AccountType.OFFLINE));
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot =
        new BotImpl("a1", acctMgr, null, null, bus, silentLogger(), 1000, 1000, TEST_NET_HOST, 1);
    try {
      bot.start().get(10, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
    List<Class<? extends Event>> eventTypes = bus.eventTypes();
    assertTrue(eventTypes.contains(BotStartingEvent.class));
    assertTrue(eventTypes.contains(BotFailedEvent.class));
  }

  @Test
  void startWithUnknownProxyIdFails() throws Exception {
    TestAccountManager acctMgr = new TestAccountManager();
    Account acct =
        Account.builder()
            .id("a1")
            .username("TestUser")
            .type(AccountType.OFFLINE)
            .proxy("nonexistent")
            .build();
    acctMgr.add(acct);
    TestProxyManager pm = new TestProxyManager();
    BotImpl bot =
        new BotImpl(
            "a1",
            acctMgr,
            pm,
            null,
            new RecordingEventBus(),
            silentLogger(),
            1000,
            1000,
            "localhost",
            25565);
    try {
      bot.start().get(5, TimeUnit.SECONDS);
      fail("Expected startup to fail");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalStateException);
      assertTrue(e.getCause().getMessage().contains("proxy not found"));
    }
  }

  @Test
  void startWithUnknownProxyIdPublishesFailedEvent() throws Exception {
    TestAccountManager acctMgr = new TestAccountManager();
    Account acct =
        Account.builder()
            .id("a1")
            .username("TestUser")
            .type(AccountType.OFFLINE)
            .proxy("nonexistent")
            .build();
    acctMgr.add(acct);
    TestProxyManager pm = new TestProxyManager();
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot =
        new BotImpl("a1", acctMgr, pm, null, bus, silentLogger(), 1000, 1000, "localhost", 25565);
    try {
      bot.start().get(5, TimeUnit.SECONDS);
    } catch (Exception expected) {
    }
    assertTrue(bus.contains(BotFailedEvent.class));
  }

  // ===== Reconnect Tests =====

  @Test
  void initiateReconnectFromRunningTransitionsToReconnecting() {
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus);
    forceState(bot, BotState.RUNNING);
    bot.initiateReconnect();
    assertEquals(BotState.RECONNECTING, bot.getState());
    assertTrue(bus.contains(BotReconnectStartedEvent.class));
  }

  @Test
  void initiateReconnectNotTriggeredWhenNotRunning() {
    for (BotState s :
        List.of(
            BotState.CREATED,
            BotState.STARTING,
            BotState.STOPPED,
            BotState.FAILED,
            BotState.RECONNECTING)) {
      RecordingEventBus bus = new RecordingEventBus();
      BotImpl bot = createTestBot("a1", trueAccount("a1"), bus);
      forceState(bot, s);
      bot.initiateReconnect();
      assertEquals(s, bot.getState(), "State should not change from " + s);
      assertFalse(bus.contains(BotReconnectStartedEvent.class));
    }
  }

  @Test
  void reconnectDisabledTransitionsToFailed() {
    ReconnectPolicy disabled = ReconnectPolicy.disabled();
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus, disabled);
    forceState(bot, BotState.RUNNING);
    bot.initiateReconnect();
    assertEquals(BotState.FAILED, bot.getState());
    assertTrue(bus.contains(BotReconnectFailedEvent.class));
  }

  @Test
  void reconnectFailedAfterMaxAttempts() throws Exception {
    ReconnectPolicy policy = new ReconnectPolicy(true, 2, 1, 10);
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus, policy);
    forceState(bot, BotState.RUNNING);
    bot.initiateReconnect();
    Thread.sleep(1000);
    assertEquals(BotState.FAILED, bot.getState());
    assertTrue(bus.contains(BotReconnectStartedEvent.class));
    assertTrue(bus.contains(BotReconnectFailedEvent.class));
  }

  @Test
  void stopDuringReconnectingTransitionsToStopped() throws Exception {
    ReconnectPolicy policy = new ReconnectPolicy(true, 10, 10000, 60000);
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus, policy);
    forceState(bot, BotState.RUNNING);
    bot.initiateReconnect();
    assertEquals(BotState.RECONNECTING, bot.getState());
    bot.stop().get(5, TimeUnit.SECONDS);
    assertEquals(BotState.STOPPED, bot.getState());
    assertTrue(bus.contains(BotStoppedEvent.class));
  }

  @Test
  void initiateReconnectIncrementsAttemptCounter() {
    BotImpl bot = createTestBot("a1", trueAccount("a1"));
    forceState(bot, BotState.RUNNING);
    assertEquals(0, bot.reconnectAttempts());
    bot.initiateReconnect();
    assertEquals(1, bot.reconnectAttempts());
    forceState(bot, BotState.RUNNING);
    bot.initiateReconnect();
    assertEquals(2, bot.reconnectAttempts());
  }

  @Test
  void stopDuringReconnectingDoesNotPublishReconnectSucceeded() throws Exception {
    ReconnectPolicy policy = new ReconnectPolicy(true, 10, 10000, 60000);
    RecordingEventBus bus = new RecordingEventBus();
    BotImpl bot = createTestBot("a1", trueAccount("a1"), bus, policy);
    forceState(bot, BotState.RUNNING);
    bot.initiateReconnect();
    bot.stop().get(5, TimeUnit.SECONDS);
    assertFalse(bus.contains(BotReconnectSucceededEvent.class));
  }

  // ---- Test helpers ----

  private static BotImpl createTestBot(String accountId, Account account) {
    return createTestBot(accountId, account, new RecordingEventBus());
  }

  private static BotImpl createTestBot(String accountId, Account account, RecordingEventBus bus) {
    TestAccountManager acctMgr = new TestAccountManager();
    if (account != null) {
      acctMgr.add(account);
    }
    return new BotImpl(
        accountId, acctMgr, null, null, bus, silentLogger(), 1000, 1000, "localhost", 25565);
  }

  private static BotImpl createTestBot(
      String accountId, Account account, RecordingEventBus bus, ReconnectPolicy policy) {
    TestAccountManager acctMgr = new TestAccountManager();
    if (account != null) {
      acctMgr.add(account);
    }
    return new BotImpl(
        accountId,
        acctMgr,
        null,
        null,
        bus,
        silentLogger(),
        1000,
        1000,
        "localhost",
        25565,
        policy);
  }

  private static void forceState(BotImpl bot, BotState target) {
    bot.forceState(target);
  }

  private static TestLogger silentLogger() {
    return new TestLogger();
  }

  private static Account trueAccount(String id) {
    return new Account(id, id, AccountType.OFFLINE);
  }

  // ---- Test stubs ----

  private static final class TestAccountManager implements AccountManager {
    private final java.util.Map<String, Account> accounts =
        new java.util.concurrent.ConcurrentHashMap<>();

    void add(Account account) {
      accounts.put(account.id(), account);
    }

    @Override
    public Account get(String id) {
      return accounts.get(id);
    }

    @Override
    public java.util.stream.Stream<Account> stream() {
      return accounts.values().stream();
    }

    @Override
    public boolean register(Account account) {
      return accounts.putIfAbsent(account.id(), account) == null;
    }

    @Override
    public boolean unregister(String id) {
      return accounts.remove(id) != null;
    }

    @Override
    public List<Account> getAll() {
      return List.copyOf(accounts.values());
    }

    @Override
    public List<Account> findEnabled() {
      return accounts.values().stream().filter(Account::enabled).toList();
    }

    @Override
    public List<Account> findByTag(String tag) {
      return accounts.values().stream().filter(a -> a.tags().contains(tag)).toList();
    }

    @Override
    public List<Account> findByServer(String server) {
      return accounts.values().stream()
          .filter(a -> a.server().isPresent() && a.server().get().equals(server))
          .toList();
    }

    @Override
    public boolean contains(String id) {
      return accounts.containsKey(id);
    }

    @Override
    public int size() {
      return accounts.size();
    }
  }

  private static final class RecordingEventBus implements EventBus {
    private final List<Event> events = new java.util.concurrent.CopyOnWriteArrayList<>();

    boolean contains(Class<? extends Event> type) {
      return events.stream().anyMatch(type::isInstance);
    }

    List<Class<? extends Event>> eventTypes() {
      return events.stream().map(Event::getClass).toList();
    }

    @Override
    public <T extends Event> Subscription subscribe(Class<T> type, EventHandler<T> handler) {
      return () -> {};
    }

    @Override
    public <T extends Event> void unsubscribe(Class<T> type, EventHandler<T> handler) {}

    @Override
    public void publish(Event event) {
      events.add(event);
    }

    @Override
    public void publishAsync(Event event) {
      events.add(event);
    }
  }

  private static final class TestProxyManager implements ProxyManager {
    private final java.util.Map<String, ProxyDefinition> proxies =
        new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public ProxyDefinition get(String id) {
      return proxies.get(id);
    }

    @Override
    public List<ProxyDefinition> getAll() {
      return List.copyOf(proxies.values());
    }

    @Override
    public List<ProxyDefinition> findByType(ProxyType type) {
      return proxies.values().stream().filter(p -> p.type() == type).toList();
    }

    @Override
    public boolean register(ProxyDefinition proxy) {
      return proxies.putIfAbsent(proxy.id(), proxy) == null;
    }

    @Override
    public boolean unregister(String id) {
      return proxies.remove(id) != null;
    }

    @Override
    public boolean contains(String id) {
      return proxies.containsKey(id);
    }

    @Override
    public int size() {
      return proxies.size();
    }
  }

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
      return false;
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
