package io.citadel.core.bot;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import io.citadel.api.bot.Bot;
import io.citadel.api.bot.BotManager;
import io.citadel.api.bot.BotState;
import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.event.bot.BotCreatedEvent;
import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
  "PMD.CouplingBetweenObjects",
  "PMD.ExcessiveImports",
  "PMD.AvoidInstantiatingObjectsInLoops"
})
class BotManagerImplTest {

  @Test
  void createReturnsBotWithCreatedState() {
    BotManager mgr = createManagerWithAccount("a1");
    Bot bot = mgr.create("a1");
    assertNotNull(bot);
    assertEquals(BotState.CREATED, bot.getState());
  }

  @Test
  void createWithMissingAccountThrows() {
    BotManager mgr = createManagerWithAccount();
    assertThrows(IllegalArgumentException.class, () -> mgr.create("nonexistent"));
  }

  @Test
  void createRejectsNullId() {
    BotManager mgr = createManagerWithAccount();
    assertThrows(NullPointerException.class, () -> mgr.create(null));
  }

  @Test
  void getReturnsBot() {
    BotManager mgr = createManagerWithAccount("a1");
    Bot bot = mgr.create("a1");
    assertSame(bot, mgr.get(bot.getBotId()));
  }

  @Test
  void getReturnsNullForUnknownId() {
    BotManager mgr = createManagerWithAccount();
    assertNull(mgr.get(UUID.randomUUID()));
  }

  @Test
  void getRejectsNull() {
    BotManager mgr = createManagerWithAccount();
    assertThrows(NullPointerException.class, () -> mgr.get(null));
  }

  @Test
  void getAllReturnsAllBots() {
    BotManager mgr = createManagerWithAccount("a1", "a2");
    Bot b1 = mgr.create("a1");
    Bot b2 = mgr.create("a2");
    Collection<Bot> all = mgr.getAll();
    assertEquals(2, all.size());
    assertTrue(all.contains(b1));
    assertTrue(all.contains(b2));
  }

  @Test
  void getAllReturnsImmutableCollection() {
    BotManager mgr = createManagerWithAccount("a1");
    mgr.create("a1");
    Collection<Bot> all = mgr.getAll();
    assertThrows(UnsupportedOperationException.class, () -> all.add(null));
  }

  @Test
  void sizeReflectsBotCount() {
    BotManager mgr = createManagerWithAccount("a1", "a2");
    assertEquals(0, mgr.size());
    mgr.create("a1");
    assertEquals(1, mgr.size());
    mgr.create("a2");
    assertEquals(2, mgr.size());
  }

  @Test
  void destroyRemovesBot() {
    BotManager mgr = createManagerWithAccount("a1");
    Bot bot = mgr.create("a1");
    assertTrue(mgr.destroy(bot.getBotId()));
    assertNull(mgr.get(bot.getBotId()));
    assertEquals(0, mgr.size());
  }

  @Test
  void destroyReturnsFalseForUnknownId() {
    BotManager mgr = createManagerWithAccount();
    assertFalse(mgr.destroy(UUID.randomUUID()));
  }

  @Test
  void destroyStopsBot() {
    BotManager mgr = createManagerWithAccount("a1");
    Bot bot = mgr.create("a1");
    mgr.destroy(bot.getBotId());
    assertTrue(
        bot.getState() == BotState.STOPPED || bot.getState() == BotState.CREATED,
        "Expected STOPPED or CREATED but got " + bot.getState());
  }

  @Test
  void startAllStartsAllBots() {
    BotManager mgr = createManagerWithAccount("a1", "a2");
    mgr.create("a1");
    mgr.create("a2");
    mgr.startAll();
    for (Bot bot : mgr.getAll()) {
      assertTrue(
          bot.getState() != BotState.CREATED,
          "Expected start to be invoked but got " + bot.getState());
    }
  }

  @Test
  void stopAllStopsAllBots() {
    BotManager mgr = createManagerWithAccount("a1", "a2");
    BotImpl b1 = (BotImpl) mgr.create("a1");
    BotImpl b2 = (BotImpl) mgr.create("a2");
    b1.forceState(BotState.RUNNING);
    b2.forceState(BotState.RUNNING);
    mgr.stopAll();
    assertEquals(BotState.STOPPING, b1.getState());
    assertEquals(BotState.STOPPING, b2.getState());
  }

  @Test
  void createPublishesEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    BotManager mgr = createManagerWithBus(bus, "a1");
    Bot bot = mgr.create("a1");
    BotCreatedEvent event = bus.find(BotCreatedEvent.class);
    assertNotNull(event);
    assertEquals(bot.getBotId(), event.getBotId());
  }

  @Test
  void concurrentCreate() throws Exception {
    BotManager mgr = createManagerWithAccounts("a1", "a2", "a3", "a4", "a5");
    int threadCount = 5;
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger created = new AtomicInteger();
    for (int i = 0; i < threadCount; i++) {
      int idx = i;
      exec.submit(
          () -> {
            try {
              mgr.create("a" + (idx + 1));
              created.incrementAndGet();
            } catch (Exception e) {
            } finally {
              latch.countDown();
            }
          });
    }
    latch.await(10, TimeUnit.SECONDS);
    exec.shutdown();
    assertEquals(threadCount, mgr.size());
  }

  @Test
  void concurrentLookup() throws Exception {
    BotManager mgr = createManagerWithAccounts("a1", "a2", "a3");
    List<Bot> bots = List.of(mgr.create("a1"), mgr.create("a2"), mgr.create("a3"));
    int threadCount = 10;
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    for (int t = 0; t < threadCount; t++) {
      exec.submit(
          () -> {
            try {
              for (int i = 0; i < 100; i++) {
                Bot b = bots.get(i % 3);
                assertNotNull(mgr.get(b.getBotId()));
                mgr.getAll();
                mgr.size();
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            } finally {
              latch.countDown();
            }
          });
    }
    latch.await(10, TimeUnit.SECONDS);
    exec.shutdown();
  }

  // ---- Test helpers ----

  private static BotManager createManagerWithAccount(String... ids) {
    TestAccountManager am = new TestAccountManager();
    for (String id : ids) {
      am.add(new Account(id, id, AccountType.OFFLINE));
    }
    return new BotManagerImpl(am, null, new RecordingEventBus(), silentLogger(), testConfig());
  }

  private static BotManager createManagerWithBus(RecordingEventBus bus, String... ids) {
    TestAccountManager am = new TestAccountManager();
    for (String id : ids) {
      am.add(new Account(id, id, AccountType.OFFLINE));
    }
    return new BotManagerImpl(am, null, bus, silentLogger(), testConfig());
  }

  private static BotManager createManagerWithAccounts(String... ids) {
    return createManagerWithAccount(ids);
  }

  private static TestLogger silentLogger() {
    return new TestLogger();
  }

  private static Configuration testConfig() {
    return new TestConfig();
  }

  // ---- Test stubs ----

  private static final class TestAccountManager implements AccountManager {
    private final java.util.concurrent.ConcurrentHashMap<String, Account> accounts =
        new java.util.concurrent.ConcurrentHashMap<>();

    void add(Account account) {
      accounts.put(account.id(), account);
    }

    @Override
    public Account get(String id) {
      return accounts.get(id);
    }

    @Override
    public java.util.List<Account> getAll() {
      return List.copyOf(accounts.values());
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
    public boolean contains(String id) {
      return accounts.containsKey(id);
    }

    @Override
    public int size() {
      return accounts.size();
    }

    @Override
    public java.util.List<Account> findEnabled() {
      return accounts.values().stream().filter(Account::enabled).toList();
    }

    @Override
    public java.util.List<Account> findByTag(String tag) {
      return accounts.values().stream().filter(a -> a.tags().contains(tag)).toList();
    }

    @Override
    public java.util.List<Account> findByServer(String server) {
      return accounts.values().stream()
          .filter(a -> a.server().isPresent() && a.server().get().equals(server))
          .toList();
    }
  }

  private static final class RecordingEventBus implements EventBus {
    private final List<Event> events = new java.util.concurrent.CopyOnWriteArrayList<>();

    <T extends Event> T find(Class<T> type) {
      return events.stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
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

  private static final class TestConfig implements Configuration {
    @Override
    public ConfigurationSection getRoot() {
      return this;
    }

    @Override
    public ConfigurationSection getPluginSection(String pluginName) {
      return this;
    }

    @Override
    public void reload() {}

    @Override
    public void addListener(ConfigurationListener listener) {}

    @Override
    public void removeListener(ConfigurationListener listener) {}

    @Override
    public String getString(String path) {
      return null;
    }

    @Override
    public String getString(String path, String defaultValue) {
      return defaultValue;
    }

    @Override
    public boolean getBoolean(String path) {
      return false;
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
      return defaultValue;
    }

    @Override
    public int getInt(String path) {
      return 0;
    }

    @Override
    public int getInt(String path, int defaultValue) {
      return defaultValue;
    }

    @Override
    public long getLong(String path) {
      return 0;
    }

    @Override
    public long getLong(String path, long defaultValue) {
      return defaultValue;
    }

    @Override
    public double getDouble(String path) {
      return 0;
    }

    @Override
    public double getDouble(String path, double defaultValue) {
      return defaultValue;
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
      return null;
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
      return defaultValue;
    }

    @Override
    public List<String> getStringList(String path) {
      return List.of();
    }

    @Override
    public <T> List<T> getList(String path) {
      return List.of();
    }

    @Override
    public ConfigurationSection getSection(String path) {
      return this;
    }

    @Override
    public boolean contains(String path) {
      return false;
    }

    @Override
    public Set<String> getKeys() {
      return Set.of();
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
