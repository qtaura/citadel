package io.citadel.core.proxy;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.event.proxy.ProxyRegisteredEvent;
import io.citadel.api.event.proxy.ProxyRemovedEvent;
import io.citadel.api.event.proxy.ProxyUpdatedEvent;
import io.citadel.api.proxy.ProxyDefinition;
import io.citadel.api.proxy.ProxyType;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationException;
import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
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
import org.junit.jupiter.api.Test;

@SuppressWarnings({
  "PMD.CouplingBetweenObjects",
  "PMD.ExcessiveImports",
  "PMD.AvoidInstantiatingObjectsInLoops",
  "PMD.AvoidUsingHardCodedIP"
})
class ProxyManagerImplTest {

  @Test
  void registerAndGet() {
    ProxyManagerImpl manager = emptyManager();
    ProxyDefinition proxy = new ProxyDefinition("p1", ProxyType.SOCKS5, "127.0.0.1", 1080);
    assertTrue(manager.register(proxy));
    assertSame(proxy, manager.get("p1"));
  }

  @Test
  void registerRejectsDuplicateId() {
    ProxyManagerImpl manager = emptyManager();
    ProxyDefinition p1 = new ProxyDefinition("dup", ProxyType.SOCKS5, "10.0.0.1", 1080);
    ProxyDefinition p2 = new ProxyDefinition("dup", ProxyType.SOCKS5, "10.0.0.2", 1081);
    assertTrue(manager.register(p1));
    assertFalse(manager.register(p2));
    assertSame(p1, manager.get("dup"));
  }

  @Test
  void unregisterRemovesProxy() {
    ProxyManagerImpl manager = emptyManager();
    ProxyDefinition p = new ProxyDefinition("x", ProxyType.SOCKS5, "10.0.0.1", 3128);
    manager.register(p);
    assertTrue(manager.unregister("x"));
    assertNull(manager.get("x"));
    assertFalse(manager.unregister("x"));
  }

  @Test
  void containsReturnsCorrectly() {
    ProxyManagerImpl manager = emptyManager();
    assertFalse(manager.contains("any"));
    manager.register(new ProxyDefinition("a", ProxyType.DIRECT, null, 0));
    assertTrue(manager.contains("a"));
  }

  @Test
  void sizeReflectsRegistrationCount() {
    ProxyManagerImpl manager = emptyManager();
    assertEquals(0, manager.size());
    manager.register(new ProxyDefinition("a", ProxyType.DIRECT, null, 0));
    assertEquals(1, manager.size());
    manager.register(new ProxyDefinition("b", ProxyType.SOCKS5, "1.2.3.4", 1080));
    assertEquals(2, manager.size());
  }

  @Test
  void getAllReturnsAllProxies() {
    ProxyManagerImpl manager = emptyManager();
    ProxyDefinition p1 = new ProxyDefinition("a", ProxyType.DIRECT, null, 0);
    ProxyDefinition p2 = new ProxyDefinition("b", ProxyType.SOCKS5, "10.0.0.1", 1080);
    manager.register(p1);
    manager.register(p2);
    List<ProxyDefinition> all = manager.getAll();
    assertEquals(2, all.size());
    assertTrue(all.contains(p1));
    assertTrue(all.contains(p2));
  }

  @Test
  void getAllReturnsImmutable() {
    ProxyManagerImpl manager = emptyManager();
    manager.register(new ProxyDefinition("a", ProxyType.DIRECT, null, 0));
    assertThrows(UnsupportedOperationException.class, () -> manager.getAll().add(null));
  }

  @Test
  void findByTypeReturnsCorrectProxies() {
    ProxyManagerImpl manager = emptyManager();
    manager.register(new ProxyDefinition("d1", ProxyType.DIRECT, null, 0));
    manager.register(new ProxyDefinition("s1", ProxyType.SOCKS5, "1.2.3.4", 1080));
    manager.register(new ProxyDefinition("h1", ProxyType.HTTP, "5.6.7.8", 3128));
    manager.register(new ProxyDefinition("s2", ProxyType.SOCKS5, "9.10.11.12", 1080));

    List<ProxyDefinition> socks = manager.findByType(ProxyType.SOCKS5);
    assertEquals(2, socks.size());

    List<ProxyDefinition> http = manager.findByType(ProxyType.HTTP);
    assertEquals(1, http.size());

    List<ProxyDefinition> direct = manager.findByType(ProxyType.DIRECT);
    assertEquals(1, direct.size());
  }

  @Test
  void registerPublishesEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    ProxyManagerImpl manager = new ProxyManagerImpl(new EmptyConfig(), bus, noOpLogger());
    ProxyDefinition p = new ProxyDefinition("p1", ProxyType.SOCKS5, "1.2.3.4", 1080);
    manager.register(p);
    ProxyRegisteredEvent event = bus.find(ProxyRegisteredEvent.class);
    assertNotNull(event);
    assertSame(p, event.getProxy());
  }

  @Test
  void unregisterPublishesEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    ProxyManagerImpl manager = new ProxyManagerImpl(new EmptyConfig(), bus, noOpLogger());
    ProxyDefinition p = new ProxyDefinition("p1", ProxyType.SOCKS5, "1.2.3.4", 1080);
    manager.register(p);
    manager.unregister("p1");
    ProxyRemovedEvent event = bus.find(ProxyRemovedEvent.class);
    assertNotNull(event);
    assertSame(p, event.getProxy());
  }

  @Test
  void unregisterNonExistentDoesNotPublishEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    ProxyManagerImpl manager = new ProxyManagerImpl(new EmptyConfig(), bus, noOpLogger());
    assertFalse(manager.unregister("nonexistent"));
    assertNull(bus.find(ProxyRemovedEvent.class));
  }

  @Test
  void directProxyAcceptsNullHostAndZeroPort() {
    ProxyDefinition direct = new ProxyDefinition("d", ProxyType.DIRECT, null, 0);
    assertEquals(ProxyType.DIRECT, direct.type());
    assertNull(direct.host());
    assertEquals(0, direct.port());
  }

  @Test
  void socksProxyRequiresHost() {
    assertThrows(
        NullPointerException.class, () -> new ProxyDefinition("s", ProxyType.SOCKS5, null, 1080));
  }

  @Test
  void socksProxyRequiresHostNotBlank() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProxyDefinition("s", ProxyType.SOCKS5, "  ", 1080));
  }

  @Test
  void socksProxyRequiresValidPortRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProxyDefinition("s", ProxyType.SOCKS5, "1.2.3.4", 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProxyDefinition("s", ProxyType.SOCKS5, "1.2.3.4", 65536));
  }

  @Test
  void httpProxyRequiresHost() {
    assertThrows(
        NullPointerException.class, () -> new ProxyDefinition("h", ProxyType.HTTP, null, 3128));
  }

  @Test
  void httpProxyRequiresValidPortRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProxyDefinition("h", ProxyType.HTTP, "1.2.3.4", -1));
  }

  @Test
  void proxyManagerRejectsNullId() {
    ProxyManagerImpl manager = emptyManager();
    assertThrows(NullPointerException.class, () -> manager.get(null));
    assertThrows(NullPointerException.class, () -> manager.unregister(null));
    assertThrows(NullPointerException.class, () -> manager.contains(null));
  }

  @Test
  void proxyManagerRejectsNullProxy() {
    ProxyManagerImpl manager = emptyManager();
    assertThrows(NullPointerException.class, () -> manager.register(null));
  }

  @Test
  void findByTypeRejectsNull() {
    ProxyManagerImpl manager = emptyManager();
    assertThrows(NullPointerException.class, () -> manager.findByType(null));
  }

  @Test
  void concurrentRegisterAndLookup() throws Exception {
    ProxyManagerImpl manager = emptyManager();
    int threadCount = 10;
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    CyclicBarrier barrier = new CyclicBarrier(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicBoolean failed = new AtomicBoolean();

    for (int i = 0; i < threadCount; i++) {
      int idx = i;
      exec.submit(
          () -> {
            try {
              barrier.await();
              ProxyDefinition p =
                  new ProxyDefinition("p" + idx, ProxyType.SOCKS5, "10.0.0." + idx, 1080 + idx);
              manager.register(p);
              ProxyDefinition retrieved = manager.get("p" + idx);
              if (retrieved == null || !retrieved.id().equals("p" + idx)) {
                failed.set(true);
              }
            } catch (Exception e) {
              failed.set(true);
            } finally {
              latch.countDown();
            }
          });
    }
    latch.await(10, TimeUnit.SECONDS);
    exec.shutdown();
    assertFalse(failed.get());
    assertEquals(threadCount, manager.size());
  }

  @Test
  void concurrentRegisterAndUnregister() throws Exception {
    ProxyManagerImpl manager = emptyManager();
    ProxyDefinition p = new ProxyDefinition("target", ProxyType.SOCKS5, "10.0.0.1", 1080);
    manager.register(p);

    int threadCount = 10;
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicBoolean found = new AtomicBoolean();
    AtomicBoolean removed = new AtomicBoolean();

    for (int i = 0; i < threadCount; i++) {
      exec.submit(
          () -> {
            try {
              if (manager.get("target") != null) {
                found.set(true);
              }
              if (manager.unregister("target")) {
                removed.set(true);
              }
            } finally {
              latch.countDown();
            }
          });
    }
    latch.await(10, TimeUnit.SECONDS);
    exec.shutdown();
    assertFalse(manager.contains("target"));
  }

  @Test
  void loadsFromConfigOnConstruction() {
    MapConfig config = new MapConfig();
    config.put("proxies.socks1.type", "SOCKS5");
    config.put("proxies.socks1.host", "10.0.0.1");
    config.put("proxies.socks1.port", 1080);
    config.put("proxies.direct1.type", "DIRECT");

    ProxyManagerImpl manager = new ProxyManagerImpl(config, noOpBus(), noOpLogger());
    assertEquals(2, manager.size());
    assertNotNull(manager.get("socks1"));
    assertNotNull(manager.get("direct1"));
    assertEquals(ProxyType.DIRECT, manager.get("direct1").type());
  }

  @Test
  void loadRejectsUnknownField() {
    MapConfig config = new MapConfig();
    config.put("proxies.bad.unknown_field", "value");
    assertThrows(
        ConfigurationException.class, () -> new ProxyManagerImpl(config, noOpBus(), noOpLogger()));
  }

  @Test
  void loadRejectsMissingType() {
    MapConfig config = new MapConfig();
    config.put("proxies.bad.host", "1.2.3.4");
    assertThrows(
        ConfigurationException.class, () -> new ProxyManagerImpl(config, noOpBus(), noOpLogger()));
  }

  @Test
  void loadRejectsMissingHostForNonDirect() {
    MapConfig config = new MapConfig();
    config.put("proxies.bad.type", "SOCKS5");
    config.put("proxies.bad.port", 1080);
    assertThrows(
        ConfigurationException.class, () -> new ProxyManagerImpl(config, noOpBus(), noOpLogger()));
  }

  @Test
  void loadRejectsInvalidPortForNonDirect() {
    MapConfig config = new MapConfig();
    config.put("proxies.bad.type", "SOCKS5");
    config.put("proxies.bad.host", "1.2.3.4");
    config.put("proxies.bad.port", 0);
    assertThrows(
        ConfigurationException.class, () -> new ProxyManagerImpl(config, noOpBus(), noOpLogger()));
  }

  @Test
  void reloadNotifiesUpdate() {
    MapConfig config = new MapConfig();
    config.put("proxies.p1.type", "DIRECT");
    RecordingEventBus bus = new RecordingEventBus();
    ProxyManagerImpl manager = new ProxyManagerImpl(config, bus, noOpLogger());
    assertEquals(1, manager.size());

    config.put("proxies.p1.type", "SOCKS5");
    config.put("proxies.p1.host", "10.0.0.1");
    config.put("proxies.p1.port", 1080);
    config.fireReload();
    assertEquals(ProxyType.SOCKS5, manager.get("p1").type());
    assertNotNull(bus.find(ProxyUpdatedEvent.class));
  }

  @Test
  void reloadRemovesDeleted() {
    MapConfig config = new MapConfig();
    config.put("proxies.p1.type", "DIRECT");
    config.put("proxies.p2.type", "DIRECT");
    ProxyManagerImpl manager = new ProxyManagerImpl(config, noOpBus(), noOpLogger());
    assertEquals(2, manager.size());

    config.removePrefix("proxies.p2");
    config.fireReload();
    assertEquals(1, manager.size());
    assertNull(manager.get("p2"));
  }

  // ---- Test helpers ----

  private static ProxyManagerImpl emptyManager() {
    return new ProxyManagerImpl(new EmptyConfig(), noOpBus(), noOpLogger());
  }

  private static RecordingEventBus noOpBus() {
    return new RecordingEventBus();
  }

  private static Logger noOpLogger() {
    return new NoOpLogger();
  }

  // ---- Test stubs ----

  private static final class EmptyConfig implements Configuration {
    @Override
    public ConfigurationSection getRoot() {
      return new MapSection();
    }

    @Override
    public ConfigurationSection getPluginSection(String pluginName) {
      return new MapSection();
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
      return new MapSection();
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

  private static final class MapConfig implements Configuration {
    private final Map<String, Object> data = new LinkedHashMap<>();
    private final List<ConfigurationListener> listeners =
        new java.util.concurrent.CopyOnWriteArrayList<>();

    void put(String path, Object value) {
      data.put(path, value);
    }

    void remove(String path) {
      data.remove(path);
    }

    void removePrefix(String prefix) {
      data.keySet().removeIf(k -> k.startsWith(prefix + ".") || k.equals(prefix));
    }

    void fireReload() {
      for (ConfigurationListener listener : listeners) {
        listener.onReload(this);
      }
    }

    @Override
    public String getString(String path) {
      Object v = data.get(path);
      return v != null ? v.toString() : null;
    }

    @Override
    public String getString(String path, String defaultValue) {
      Object v = data.get(path);
      return v != null ? v.toString() : defaultValue;
    }

    @Override
    public int getInt(String path) {
      Object v = data.get(path);
      return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    @Override
    public int getInt(String path, int defaultValue) {
      Object v = data.get(path);
      return v instanceof Number ? ((Number) v).intValue() : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
      Object v = data.get(path);
      if (v == null) {
        return null;
      }
      return Enum.valueOf(enumClass, v.toString());
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
      Object v = data.get(path);
      if (v == null) {
        return defaultValue;
      }
      return Enum.valueOf(enumClass, v.toString());
    }

    @Override
    public boolean contains(String path) {
      return data.containsKey(path);
    }

    @Override
    public Set<String> getKeys() {
      return data.keySet();
    }

    @Override
    public ConfigurationSection getSection(String path) {
      return new MapConfigSection(data, path);
    }

    @Override
    public void reload() {
      fireReload();
    }

    @Override
    public void addListener(ConfigurationListener listener) {
      listeners.add(listener);
    }

    @Override
    public void removeListener(ConfigurationListener listener) {
      listeners.remove(listener);
    }

    @Override
    public ConfigurationSection getRoot() {
      return this;
    }

    @Override
    public ConfigurationSection getPluginSection(String pluginName) {
      return this;
    }

    @Override
    public long getLong(String path) {
      return getInt(path);
    }

    @Override
    public long getLong(String path, long defaultValue) {
      return getInt(path, (int) defaultValue);
    }

    @Override
    public double getDouble(String path) {
      return getInt(path);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
      return getInt(path, (int) defaultValue);
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
    public List<String> getStringList(String path) {
      return List.of();
    }

    @Override
    public <T> List<T> getList(String path) {
      return List.of();
    }
  }

  private static final class MapConfigSection implements ConfigurationSection {
    private final Map<String, Object> data;
    private final String prefix;

    MapConfigSection(Map<String, Object> data, String prefix) {
      this.data = data;
      this.prefix = prefix.isEmpty() ? "" : prefix + ".";
    }

    private String prefixed(String path) {
      return prefix + path;
    }

    @Override
    public String getString(String path) {
      Object v = data.get(prefixed(path));
      return v != null ? v.toString() : null;
    }

    @Override
    public String getString(String path, String defaultValue) {
      Object v = data.get(prefixed(path));
      return v != null ? v.toString() : defaultValue;
    }

    @Override
    public int getInt(String path) {
      Object v = data.get(prefixed(path));
      return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    @Override
    public int getInt(String path, int defaultValue) {
      Object v = data.get(prefixed(path));
      return v instanceof Number ? ((Number) v).intValue() : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
      Object v = data.get(prefixed(path));
      if (v == null) {
        return null;
      }
      return Enum.valueOf(enumClass, v.toString());
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
      Object v = data.get(prefixed(path));
      if (v == null) {
        return defaultValue;
      }
      return Enum.valueOf(enumClass, v.toString());
    }

    @Override
    public boolean contains(String path) {
      return data.containsKey(prefixed(path));
    }

    @Override
    public Set<String> getKeys() {
      Set<String> keys = new java.util.LinkedHashSet<>();
      String p = prefix;
      for (String key : data.keySet()) {
        if (key.startsWith(p)) {
          String remainder = key.substring(p.length());
          int dot = remainder.indexOf('.');
          if (dot > 0) {
            keys.add(remainder.substring(0, dot));
          } else {
            keys.add(remainder);
          }
        }
      }
      return keys;
    }

    @Override
    public ConfigurationSection getSection(String path) {
      return new MapConfigSection(data, prefixed(path));
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
    public long getLong(String path) {
      return getInt(path);
    }

    @Override
    public long getLong(String path, long defaultValue) {
      return getInt(path, (int) defaultValue);
    }

    @Override
    public double getDouble(String path) {
      return getInt(path);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
      return getInt(path, (int) defaultValue);
    }

    @Override
    public List<String> getStringList(String path) {
      return List.of();
    }

    @Override
    public <T> List<T> getList(String path) {
      return List.of();
    }
  }

  private static final class NoOpLogger implements Logger {
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

  private static final class MapSection implements ConfigurationSection {
    MapSection() {}

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
}
