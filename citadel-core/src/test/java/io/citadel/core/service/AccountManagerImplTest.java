package io.citadel.core.service;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.event.account.AccountRegisteredEvent;
import io.citadel.api.event.account.AccountRemovedEvent;
import io.citadel.api.event.account.AccountUpdatedEvent;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
  "PMD.CouplingBetweenObjects",
  "PMD.ExcessiveImports",
  "PMD.AvoidInstantiatingObjectsInLoops"
})
class AccountManagerImplTest {

  @Test
  void registerAndGet() {
    AccountManagerImpl manager = emptyManager();
    Account account =
        Account.builder().id("a1").username("Steve").type(AccountType.OFFLINE).build();
    assertTrue(manager.register(account));
    assertSame(account, manager.get("a1"));
  }

  @Test
  void registerRejectsDuplicateId() {
    AccountManagerImpl manager = emptyManager();
    Account a1 = Account.builder().id("dup").username("first").type(AccountType.OFFLINE).build();
    Account a2 = Account.builder().id("dup").username("second").type(AccountType.OFFLINE).build();
    assertTrue(manager.register(a1));
    assertFalse(manager.register(a2));
    assertSame(a1, manager.get("dup"));
  }

  @Test
  void unregisterRemovesAccount() {
    AccountManagerImpl manager = emptyManager();
    Account a = Account.builder().id("x").username("X").type(AccountType.OFFLINE).build();
    manager.register(a);
    assertTrue(manager.unregister("x"));
    assertNull(manager.get("x"));
    assertFalse(manager.unregister("x"));
  }

  @Test
  void containsReturnsCorrectly() {
    AccountManagerImpl manager = emptyManager();
    assertFalse(manager.contains("any"));
    manager.register(Account.builder().id("a").username("A").type(AccountType.OFFLINE).build());
    assertTrue(manager.contains("a"));
  }

  @Test
  void sizeReflectsRegistrationCount() {
    AccountManagerImpl manager = emptyManager();
    assertEquals(0, manager.size());
    manager.register(Account.builder().id("a").username("A").type(AccountType.OFFLINE).build());
    assertEquals(1, manager.size());
    manager.register(Account.builder().id("b").username("B").type(AccountType.OFFLINE).build());
    assertEquals(2, manager.size());
    manager.unregister("a");
    assertEquals(1, manager.size());
  }

  @Test
  void getAllReturnsImmutableSnapshot() {
    AccountManagerImpl manager = emptyManager();
    Account a = Account.builder().id("a").username("A").type(AccountType.OFFLINE).build();
    manager.register(a);
    List<Account> all = manager.getAll();
    assertEquals(1, all.size());
    assertThrows(UnsupportedOperationException.class, () -> all.add(a));
  }

  @Test
  void streamProvidesAllAccounts() {
    AccountManagerImpl manager = emptyManager();
    manager.register(Account.builder().id("a").username("A").type(AccountType.OFFLINE).build());
    manager.register(Account.builder().id("b").username("B").type(AccountType.OFFLINE).build());
    assertEquals(2, manager.stream().count());
  }

  @Test
  void findEnabledReturnsOnlyEnabledAccounts() {
    AccountManagerImpl manager = emptyManager();
    manager.register(
        Account.builder().id("a").username("A").type(AccountType.OFFLINE).enabled(true).build());
    manager.register(
        Account.builder().id("b").username("B").type(AccountType.OFFLINE).enabled(false).build());
    manager.register(
        Account.builder().id("c").username("C").type(AccountType.OFFLINE).enabled(true).build());
    List<Account> enabled = manager.findEnabled();
    assertEquals(2, enabled.size());
    assertTrue(enabled.stream().allMatch(Account::enabled));
  }

  @Test
  void findByTagReturnsMatchingAccounts() {
    AccountManagerImpl manager = emptyManager();
    manager.register(
        Account.builder()
            .id("a")
            .username("A")
            .type(AccountType.OFFLINE)
            .tags(List.of("printer", "carpet"))
            .build());
    manager.register(
        Account.builder()
            .id("b")
            .username("B")
            .type(AccountType.OFFLINE)
            .tags(List.of("printer"))
            .build());
    manager.register(
        Account.builder()
            .id("c")
            .username("C")
            .type(AccountType.OFFLINE)
            .tags(List.of("builder"))
            .build());
    assertEquals(2, manager.findByTag("printer").size());
    assertEquals(1, manager.findByTag("carpet").size());
    assertEquals(0, manager.findByTag("nonexistent").size());
  }

  @Test
  void findByServerReturnsMatchingAccounts() {
    AccountManagerImpl manager = emptyManager();
    manager.register(
        Account.builder()
            .id("a")
            .username("A")
            .type(AccountType.OFFLINE)
            .server("citadel")
            .build());
    manager.register(
        Account.builder()
            .id("b")
            .username("B")
            .type(AccountType.OFFLINE)
            .server("citadel")
            .build());
    manager.register(Account.builder().id("c").username("C").type(AccountType.OFFLINE).build());
    assertEquals(2, manager.findByServer("citadel").size());
    assertEquals(0, manager.findByServer("unknown").size());
  }

  @Test
  void registerPublishesEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    AccountManagerImpl manager = new AccountManagerImpl(emptyConfig(), bus, silentLogger());
    Account account = Account.builder().id("a").username("A").type(AccountType.OFFLINE).build();
    manager.register(account);
    assertTrue(bus.contains(AccountRegisteredEvent.class));
  }

  @Test
  void unregisterPublishesEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    AccountManagerImpl manager = new AccountManagerImpl(emptyConfig(), bus, silentLogger());
    Account account = Account.builder().id("a").username("A").type(AccountType.OFFLINE).build();
    manager.register(account);
    bus.clear();
    manager.unregister("a");
    assertTrue(bus.contains(AccountRemovedEvent.class));
  }

  @Test
  void registerEventCarriesCorrectAccount() {
    RecordingEventBus bus = new RecordingEventBus();
    AccountManagerImpl manager = new AccountManagerImpl(emptyConfig(), bus, silentLogger());
    Account account = Account.builder().id("evt").username("Evt").type(AccountType.OFFLINE).build();
    manager.register(account);
    AccountRegisteredEvent event = bus.find(AccountRegisteredEvent.class);
    assertNotNull(event);
    assertSame(account, event.getAccount());
  }

  @Test
  void unregisterEventCarriesCorrectAccount() {
    RecordingEventBus bus = new RecordingEventBus();
    AccountManagerImpl manager = new AccountManagerImpl(emptyConfig(), bus, silentLogger());
    Account account = Account.builder().id("evt").username("Evt").type(AccountType.OFFLINE).build();
    manager.register(account);
    bus.clear();
    manager.unregister("evt");
    AccountRemovedEvent event = bus.find(AccountRemovedEvent.class);
    assertNotNull(event);
    assertEquals("evt", event.getAccount().id());
  }

  @Test
  void duplicateRegistrationDoesNotPublishEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    AccountManagerImpl manager = new AccountManagerImpl(emptyConfig(), bus, silentLogger());
    Account a = Account.builder().id("a").username("A").type(AccountType.OFFLINE).build();
    manager.register(a);
    bus.clear();
    manager.register(a);
    assertFalse(bus.contains(AccountRegisteredEvent.class));
  }

  @Test
  void loadsAccountsFromConfigOnConstruction() {
    Map<String, Object> accountsData = new LinkedHashMap<>();
    accountsData.put("username", "Builder01");
    accountsData.put("type", "OFFLINE");
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("accounts", Map.of("builder", accountsData));
    ConfigWithData config = new ConfigWithData(root);
    RecordingEventBus bus = new RecordingEventBus();
    AccountManagerImpl manager = new AccountManagerImpl(config, bus, silentLogger());
    Account account = manager.get("builder");
    assertNotNull(account);
    assertEquals("Builder01", account.username());
    assertEquals(AccountType.OFFLINE, account.type());
  }

  @Test
  void loadsFullAccountMetadataFromConfig() {
    Map<String, Object> def = new LinkedHashMap<>();
    def.put("username", "Printer01");
    def.put("type", "OFFLINE");
    def.put("proxy", "germany-1");
    def.put("server", "citadel");
    def.put("tags", List.of("printer", "carpet"));
    def.put("enabled", true);
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("accounts", Map.of("printer1", def));
    ConfigWithData config = new ConfigWithData(root);
    AccountManagerImpl manager =
        new AccountManagerImpl(config, new RecordingEventBus(), silentLogger());
    Account account = manager.get("printer1");
    assertNotNull(account);
    assertEquals("Printer01", account.username());
    assertEquals(AccountType.OFFLINE, account.type());
    assertTrue(account.server().isPresent());
    assertEquals("citadel", account.server().get());
    assertTrue(account.proxy().isPresent());
    assertEquals("germany-1", account.proxy().get());
    assertEquals(2, account.tags().size());
    assertTrue(account.tags().contains("printer"));
    assertTrue(account.tags().contains("carpet"));
    assertTrue(account.enabled());
  }

  @Test
  void loadsAccountsWithDefaultValuesWhenOptionalFieldsMissing() {
    Map<String, Object> def = new LinkedHashMap<>();
    def.put("username", "Default");
    def.put("type", "OFFLINE");
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("accounts", Map.of("default", def));
    ConfigWithData config = new ConfigWithData(root);
    AccountManagerImpl manager =
        new AccountManagerImpl(config, new RecordingEventBus(), silentLogger());
    Account account = manager.get("default");
    assertTrue(account.enabled());
    assertTrue(account.tags().isEmpty());
    assertTrue(account.server().isEmpty());
    assertTrue(account.proxy().isEmpty());
  }

  @Test
  void rejectsUnknownConfigFields() {
    Map<String, Object> def = new LinkedHashMap<>();
    def.put("username", "Bad");
    def.put("type", "OFFLINE");
    def.put("unknown_field", "value");
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("accounts", Map.of("bad", def));
    ConfigWithData config = new ConfigWithData(root);
    RecordingEventBus bus = new RecordingEventBus();
    assertThrows(RuntimeException.class, () -> new AccountManagerImpl(config, bus, silentLogger()));
  }

  @Test
  void configReloadReplacesAccounts() {
    Map<String, Object> def1 = new LinkedHashMap<>();
    def1.put("username", "First");
    def1.put("type", "OFFLINE");
    Map<String, Object> root1 = new LinkedHashMap<>();
    root1.put("accounts", Map.of("a1", def1));
    ReloadableConfig config = new ReloadableConfig(root1);
    RecordingEventBus bus = new RecordingEventBus();
    AccountManagerImpl manager = new AccountManagerImpl(config, bus, silentLogger());
    assertEquals(1, manager.size());
    assertNotNull(manager.get("a1"));
    Map<String, Object> def2 = new LinkedHashMap<>();
    def2.put("username", "Second");
    def2.put("type", "OFFLINE");
    Map<String, Object> root2 = new LinkedHashMap<>();
    root2.put("accounts", Map.of("a2", def2));
    config.data = root2;
    config.fireReload();
    assertEquals(1, manager.size());
    assertNull(manager.get("a1"));
    assertNotNull(manager.get("a2"));
  }

  @Test
  void configReloadPublishesUpdateEventForChangedAccounts() {
    Map<String, Object> def1 = new LinkedHashMap<>();
    def1.put("username", "Old");
    def1.put("type", "OFFLINE");
    Map<String, Object> root1 = new LinkedHashMap<>();
    root1.put("accounts", Map.of("a", def1));
    ReloadableConfig config = new ReloadableConfig(root1);
    RecordingEventBus bus = new RecordingEventBus();
    new AccountManagerImpl(config, bus, silentLogger());
    bus.clear();
    Map<String, Object> def2 = new LinkedHashMap<>();
    def2.put("username", "New");
    def2.put("type", "OFFLINE");
    Map<String, Object> root2 = new LinkedHashMap<>();
    root2.put("accounts", Map.of("a", def2));
    config.data = root2;
    config.fireReload();
    assertTrue(bus.contains(AccountUpdatedEvent.class));
  }

  @Test
  void nullIdIsRejected() {
    AccountManagerImpl manager = emptyManager();
    assertThrows(NullPointerException.class, () -> manager.get(null));
    assertThrows(NullPointerException.class, () -> manager.contains(null));
    assertThrows(NullPointerException.class, () -> manager.unregister(null));
  }

  @Test
  void nullAccountIsRejected() {
    AccountManagerImpl manager = emptyManager();
    assertThrows(NullPointerException.class, () -> manager.register(null));
  }

  @Test
  void nullTagIsRejected() {
    AccountManagerImpl manager = emptyManager();
    assertThrows(NullPointerException.class, () -> manager.findByTag(null));
  }

  @Test
  void nullServerIsRejected() {
    AccountManagerImpl manager = emptyManager();
    assertThrows(NullPointerException.class, () -> manager.findByServer(null));
  }

  @Test
  void accountRequiresNonNullType() {
    assertThrows(NullPointerException.class, () -> new Account("id", "user", null));
  }

  @Test
  void accountRequiresNonBlankId() {
    assertThrows(
        IllegalArgumentException.class, () -> new Account("", "user", AccountType.OFFLINE));
  }

  @Test
  void accountRequiresNonBlankUsername() {
    assertThrows(IllegalArgumentException.class, () -> new Account("id", "", AccountType.OFFLINE));
  }

  @Test
  void findEnabledReturnsImmutableList() {
    AccountManagerImpl manager = emptyManager();
    manager.register(Account.builder().id("a").username("A").type(AccountType.OFFLINE).build());
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            manager
                .findEnabled()
                .add(Account.builder().id("b").username("B").type(AccountType.OFFLINE).build()));
  }

  @Test
  void findByTagReturnsImmutableList() {
    AccountManagerImpl manager = emptyManager();
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            manager
                .findByTag("x")
                .add(Account.builder().id("a").username("A").type(AccountType.OFFLINE).build()));
  }

  @Test
  void findByServerReturnsImmutableList() {
    AccountManagerImpl manager = emptyManager();
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            manager
                .findByServer("s")
                .add(Account.builder().id("a").username("A").type(AccountType.OFFLINE).build()));
  }

  @Test
  void concurrentRegistration() throws Exception {
    AccountManagerImpl manager = emptyManager();
    int threadCount = 10;
    CyclicBarrier barrier = new CyclicBarrier(threadCount);
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    AtomicInteger successCount = new AtomicInteger();
    for (int i = 0; i < threadCount; i++) {
      int id = i;
      exec.submit(
          () -> {
            try {
              barrier.await();
              Account a =
                  Account.builder()
                      .id("c" + id)
                      .username("user" + id)
                      .type(AccountType.OFFLINE)
                      .build();
              if (manager.register(a)) {
                successCount.incrementAndGet();
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }
    exec.shutdown();
    assertTrue(exec.awaitTermination(10, TimeUnit.SECONDS));
    assertEquals(threadCount, successCount.get());
    assertEquals(threadCount, manager.size());
  }

  @Test
  void concurrentLookup() throws Exception {
    AccountManagerImpl manager = emptyManager();
    for (int i = 0; i < 100; i++) {
      Account a =
          Account.builder().id("a" + i).username("user" + i).type(AccountType.OFFLINE).build();
      manager.register(a);
    }
    int threadCount = 10;
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    AtomicBoolean anyError = new AtomicBoolean(false);
    CountDownLatch latch = new CountDownLatch(threadCount);
    for (int t = 0; t < threadCount; t++) {
      exec.submit(
          () -> {
            try {
              for (int i = 0; i < 1000; i++) {
                int idx = i % 100;
                Account a = manager.get("a" + idx);
                if (a == null) {
                  anyError.set(true);
                }
                manager.getAll();
                manager.size();
                manager.contains("a" + idx);
                manager.findEnabled();
              }
            } catch (Exception e) {
              anyError.set(true);
            } finally {
              latch.countDown();
            }
          });
    }
    latch.await(30, TimeUnit.SECONDS);
    exec.shutdown();
    assertFalse(anyError.get());
  }

  @Test
  void concurrentRegisterAndUnregister() throws Exception {
    AccountManagerImpl manager = emptyManager();
    int threadCount = 20;
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    CyclicBarrier barrier = new CyclicBarrier(threadCount);
    AtomicInteger registered = new AtomicInteger();
    for (int i = 0; i < threadCount; i++) {
      int id = i;
      exec.submit(
          () -> {
            try {
              barrier.await();
              Account a =
                  Account.builder()
                      .id("t" + id)
                      .username("u" + id)
                      .type(AccountType.OFFLINE)
                      .build();
              if (manager.register(a)) {
                registered.incrementAndGet();
              }
              manager.get("t" + id);
              manager.unregister("t" + id);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }
    exec.shutdown();
    assertTrue(exec.awaitTermination(10, TimeUnit.SECONDS));
    assertEquals(0, manager.size());
  }

  @Test
  void accountMetadataWithMicrosoftType() {
    AccountManagerImpl manager = emptyManager();
    Account a =
        Account.builder()
            .id("ms")
            .username("MSUser")
            .type(AccountType.MICROSOFT)
            .proxy("proxy1")
            .server("hypixel")
            .tags(List.of("premium"))
            .enabled(false)
            .build();
    assertTrue(manager.register(a));
    Account retrieved = manager.get("ms");
    assertEquals(AccountType.MICROSOFT, retrieved.type());
    assertFalse(retrieved.enabled());
    assertTrue(retrieved.server().isPresent());
    assertEquals("hypixel", retrieved.server().get());
  }

  @Test
  void yamlLoadPreservesMultipleAccounts() {
    Map<String, Object> a1 = new LinkedHashMap<>();
    a1.put("username", "User1");
    a1.put("type", "OFFLINE");
    Map<String, Object> a2 = new LinkedHashMap<>();
    a2.put("username", "User2");
    a2.put("type", "OFFLINE");
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("accounts", Map.of("one", a1, "two", a2));
    ConfigWithData config = new ConfigWithData(root);
    AccountManagerImpl manager =
        new AccountManagerImpl(config, new RecordingEventBus(), silentLogger());
    assertEquals(2, manager.size());
    assertNotNull(manager.get("one"));
    assertNotNull(manager.get("two"));
  }

  @Test
  void accountBuilderProducesCorrectAccount() {
    Account a =
        Account.builder()
            .id("test")
            .username("TestUser")
            .type(AccountType.OFFLINE)
            .server("s")
            .proxy("p")
            .tags(List.of("t1", "t2"))
            .enabled(false)
            .build();
    assertEquals("test", a.id());
    assertEquals("TestUser", a.username());
    assertEquals(AccountType.OFFLINE, a.type());
    assertTrue(a.server().isPresent());
    assertEquals("s", a.server().get());
    assertTrue(a.proxy().isPresent());
    assertEquals("p", a.proxy().get());
    assertEquals(2, a.tags().size());
    assertFalse(a.enabled());
  }

  @Test
  void accountIsImmutable() {
    List<String> tags = new ArrayList<>(List.of("a"));
    Account a =
        Account.builder().id("id").username("user").type(AccountType.OFFLINE).tags(tags).build();
    tags.add("b");
    assertEquals(1, a.tags().size());
    assertThrows(UnsupportedOperationException.class, () -> a.tags().add("c"));
  }

  // ---- Test helpers ----

  private static AccountManagerImpl emptyManager() {
    return new AccountManagerImpl(emptyConfig(), new RecordingEventBus(), silentLogger());
  }

  private static ConfigWithData emptyConfig() {
    return new ConfigWithData(Map.of("accounts", Map.of()));
  }

  private static TestLogger silentLogger() {
    return new TestLogger();
  }

  private static final class RecordingEventBus implements EventBus {
    private final List<Event> events = new ArrayList<>();

    boolean contains(Class<? extends Event> type) {
      return events.stream().anyMatch(type::isInstance);
    }

    <T extends Event> T find(Class<T> type) {
      return events.stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
    }

    void clear() {
      events.clear();
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

  private static final class TestSection implements ConfigurationSection {
    private final Map<String, Object> data;

    TestSection(Map<String, Object> data) {
      this.data = data;
    }

    @Override
    public String getString(String path) {
      return stringAt(path);
    }

    @Override
    public String getString(String path, String defaultValue) {
      Object value = resolve(path);
      return value == null ? defaultValue : stringAt(path);
    }

    @Override
    public boolean getBoolean(String path) {
      return boolAt(path);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
      Object value = resolve(path);
      return value == null ? defaultValue : boolAt(path);
    }

    @Override
    public int getInt(String path) {
      return intAt(path);
    }

    @Override
    public int getInt(String path, int defaultValue) {
      Object value = resolve(path);
      return value == null ? defaultValue : intAt(path);
    }

    @Override
    public long getLong(String path) {
      return longAt(path);
    }

    @Override
    public long getLong(String path, long defaultValue) {
      Object value = resolve(path);
      return value == null ? defaultValue : longAt(path);
    }

    @Override
    public double getDouble(String path) {
      return doubleAt(path);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
      Object value = resolve(path);
      return value == null ? defaultValue : doubleAt(path);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
      Object value = resolve(path);
      if (value == null) {
        throw new RuntimeException("Missing value at " + path);
      }
      String name = value.toString();
      try {
        return Enum.valueOf(enumClass, name);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(
            "Invalid enum value '" + name + "' for " + enumClass.getSimpleName(), e);
      }
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
      Object value = resolve(path);
      if (value == null) {
        return defaultValue;
      }
      return getEnum(path, enumClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
      Object value = resolve(path);
      if (value instanceof List) {
        return ((List<Object>) value).stream().map(Object::toString).toList();
      }
      throw new RuntimeException("Not a list at " + path);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String path) {
      Object value = resolve(path);
      if (value instanceof List) {
        return (List<T>) value;
      }
      throw new RuntimeException("Not a list at " + path);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationSection getSection(String path) {
      Object value = resolve(path);
      if (value == null) {
        return new TestSection(Map.of());
      }
      if (value instanceof Map) {
        return new TestSection((Map<String, Object>) value);
      }
      throw new RuntimeException("Not a section at " + path);
    }

    @Override
    public boolean contains(String path) {
      return resolve(path) != null;
    }

    @Override
    public Set<String> getKeys() {
      return data.keySet();
    }

    @SuppressWarnings("unchecked")
    private Object resolve(String path) {
      String[] parts = path.split("\\.");
      Map<String, Object> current = data;
      for (int i = 0; i < parts.length - 1; i++) {
        Object next = current.get(parts[i]);
        if (!(next instanceof Map)) {
          return null;
        }
        current = (Map<String, Object>) next;
      }
      return current.get(parts[parts.length - 1]);
    }

    private String stringAt(String path) {
      Object value = resolve(path);
      if (value == null) {
        throw new RuntimeException("Missing value at " + path);
      }
      return value.toString();
    }

    private boolean boolAt(String path) {
      Object value = resolve(path);
      if (value == null) {
        throw new RuntimeException("Missing value at " + path);
      }
      return (Boolean) value;
    }

    private int intAt(String path) {
      Object value = resolve(path);
      if (value == null) {
        throw new RuntimeException("Missing value at " + path);
      }
      return ((Number) value).intValue();
    }

    private long longAt(String path) {
      Object value = resolve(path);
      if (value == null) {
        throw new RuntimeException("Missing value at " + path);
      }
      return ((Number) value).longValue();
    }

    private double doubleAt(String path) {
      Object value = resolve(path);
      if (value == null) {
        throw new RuntimeException("Missing value at " + path);
      }
      return ((Number) value).doubleValue();
    }
  }

  private static final class ConfigWithData implements Configuration {
    private final Map<String, Object> root;

    ConfigWithData(Map<String, Object> root) {
      this.root = deepUnmodifiable(root);
    }

    @Override
    public ConfigurationSection getRoot() {
      return new TestSection(root);
    }

    @Override
    public ConfigurationSection getPluginSection(String pluginName) {
      return getSection("plugins." + pluginName);
    }

    @Override
    public void reload() {}

    @Override
    public void addListener(ConfigurationListener listener) {}

    @Override
    public void removeListener(ConfigurationListener listener) {}

    @Override
    public String getString(String path) {
      return getRoot().getString(path);
    }

    @Override
    public String getString(String path, String defaultValue) {
      return getRoot().getString(path, defaultValue);
    }

    @Override
    public boolean getBoolean(String path) {
      return getRoot().getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
      return getRoot().getBoolean(path, defaultValue);
    }

    @Override
    public int getInt(String path) {
      return getRoot().getInt(path);
    }

    @Override
    public int getInt(String path, int defaultValue) {
      return getRoot().getInt(path, defaultValue);
    }

    @Override
    public long getLong(String path) {
      return getRoot().getLong(path);
    }

    @Override
    public long getLong(String path, long defaultValue) {
      return getRoot().getLong(path, defaultValue);
    }

    @Override
    public double getDouble(String path) {
      return getRoot().getDouble(path);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
      return getRoot().getDouble(path, defaultValue);
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
      return getRoot().getEnum(path, enumClass);
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
      return getRoot().getEnum(path, enumClass, defaultValue);
    }

    @Override
    public List<String> getStringList(String path) {
      return getRoot().getStringList(path);
    }

    @Override
    public <T> List<T> getList(String path) {
      return getRoot().getList(path);
    }

    @Override
    public ConfigurationSection getSection(String path) {
      return getRoot().getSection(path);
    }

    @Override
    public boolean contains(String path) {
      return getRoot().contains(path);
    }

    @Override
    public Set<String> getKeys() {
      return getRoot().getKeys();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepUnmodifiable(Map<String, Object> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof Map) {
          result.put(entry.getKey(), deepUnmodifiable((Map<String, Object>) value));
        } else if (value instanceof List) {
          result.put(entry.getKey(), List.copyOf((List<Object>) value));
        } else {
          result.put(entry.getKey(), value);
        }
      }
      return Map.copyOf(result);
    }
  }

  private static final class ReloadableConfig implements Configuration {
    volatile Map<String, Object> data;
    private final List<ConfigurationListener> listeners = new ArrayList<>();

    ReloadableConfig(Map<String, Object> data) {
      this.data = deepCopy(data);
    }

    void fireReload() {
      for (ConfigurationListener l : listeners) {
        l.onReload(this);
      }
    }

    @Override
    public ConfigurationSection getRoot() {
      return new TestSection(data);
    }

    @Override
    public ConfigurationSection getPluginSection(String pluginName) {
      return getSection("plugins." + pluginName);
    }

    @Override
    public void reload() {}

    @Override
    public void addListener(ConfigurationListener listener) {
      listeners.add(listener);
    }

    @Override
    public void removeListener(ConfigurationListener listener) {
      listeners.remove(listener);
    }

    @Override
    public String getString(String path) {
      return getRoot().getString(path);
    }

    @Override
    public String getString(String path, String defaultValue) {
      return getRoot().getString(path, defaultValue);
    }

    @Override
    public boolean getBoolean(String path) {
      return getRoot().getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
      return getRoot().getBoolean(path, defaultValue);
    }

    @Override
    public int getInt(String path) {
      return getRoot().getInt(path);
    }

    @Override
    public int getInt(String path, int defaultValue) {
      return getRoot().getInt(path, defaultValue);
    }

    @Override
    public long getLong(String path) {
      return getRoot().getLong(path);
    }

    @Override
    public long getLong(String path, long defaultValue) {
      return getRoot().getLong(path, defaultValue);
    }

    @Override
    public double getDouble(String path) {
      return getRoot().getDouble(path);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
      return getRoot().getDouble(path, defaultValue);
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
      return getRoot().getEnum(path, enumClass);
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
      return getRoot().getEnum(path, enumClass, defaultValue);
    }

    @Override
    public List<String> getStringList(String path) {
      return getRoot().getStringList(path);
    }

    @Override
    public <T> List<T> getList(String path) {
      return getRoot().getList(path);
    }

    @Override
    public ConfigurationSection getSection(String path) {
      return getRoot().getSection(path);
    }

    @Override
    public boolean contains(String path) {
      return getRoot().contains(path);
    }

    @Override
    public Set<String> getKeys() {
      return getRoot().getKeys();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopy(Map<String, Object> original) {
      Map<String, Object> copy = new LinkedHashMap<>();
      for (Map.Entry<String, Object> e : original.entrySet()) {
        if (e.getValue() instanceof Map) {
          copy.put(e.getKey(), deepCopy((Map<String, Object>) e.getValue()));
        } else if (e.getValue() instanceof List) {
          copy.put(e.getKey(), List.copyOf((List<Object>) e.getValue()));
        } else {
          copy.put(e.getKey(), e.getValue());
        }
      }
      return copy;
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
