package io.citadel.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.event.EventBus;
import io.citadel.api.service.Logger;
import io.citadel.core.bootstrap.ServiceRegistry;
import io.citadel.core.event.EventBusImpl;
import io.citadel.core.logging.LoggingService;
import io.citadel.core.plugin.testplugin.FailingOnDisablePlugin;
import io.citadel.core.plugin.testplugin.FailingOnEnablePlugin;
import io.citadel.core.plugin.testplugin.FailingOnLoadPlugin;
import io.citadel.core.plugin.testplugin.SubscribeOnEnablePlugin;
import io.citadel.core.plugin.testplugin.TestPlugin;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginManagerTest {

  @TempDir Path pluginsDir;

  @TempDir Path pluginsDataDir;

  private ServiceRegistry serviceRegistry;
  private LoggingService loggingService;

  @BeforeEach
  void setUp() {
    serviceRegistry = new ServiceRegistry();
    loggingService = new LoggingService();
  }

  @Test
  void loadPluginsWithNoJarsReturnsEmptyList() {
    PluginManager manager = createManager();
    List<PluginDescriptor> descriptors = manager.loadPlugins();
    assertTrue(descriptors.isEmpty());
    assertTrue(manager.getPlugins().isEmpty());
  }

  @Test
  void loadPluginsWithValidJarReturnsDescriptor() throws Exception {
    PluginJarHelper.createPluginJar(TestPlugin.class, pluginsDir);
    PluginManager manager = createManager();
    List<PluginDescriptor> descriptors = manager.loadPlugins();
    assertEquals(1, descriptors.size());
    PluginDescriptor desc = descriptors.get(0);
    assertEquals("TestPlugin", desc.getName());
    assertEquals(PluginState.LOADED, desc.getState());
    assertNotNull(desc.getInstance());
    assertNotNull(desc.getClassLoader());
  }

  @Test
  void enablePluginsTransitionsToEnabled() throws Exception {
    PluginJarHelper.createPluginJar(TestPlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();
    manager.enablePlugins();

    PluginDescriptor desc = manager.getPlugins().get(0);
    assertEquals(PluginState.ENABLED, desc.getState());
  }

  @Test
  void fullLifecycleCompletesWithoutError() throws Exception {
    PluginJarHelper.createPluginJar(TestPlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();
    manager.enablePlugins();
    manager.disablePlugins();

    assertTrue(manager.getPlugins().isEmpty());
  }

  @Test
  void pluginThatFailsOnLoadHasErrorState() throws Exception {
    PluginJarHelper.createPluginJar(FailingOnLoadPlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();

    List<PluginDescriptor> descriptors = manager.getPlugins();
    assertEquals(1, descriptors.size());
    assertEquals(PluginState.ERROR, descriptors.get(0).getState());
  }

  @Test
  void pluginThatFailsOnEnableHasErrorState() throws Exception {
    PluginJarHelper.createPluginJar(FailingOnEnablePlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();
    manager.enablePlugins();

    PluginDescriptor desc = manager.getPlugins().get(0);
    assertEquals(PluginState.ERROR, desc.getState());
  }

  @Test
  void pluginThatFailsOnDisableDoesNotThrow() throws Exception {
    PluginJarHelper.createPluginJar(FailingOnDisablePlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();
    manager.enablePlugins();
    manager.disablePlugins();

    assertTrue(manager.getPlugins().isEmpty());
  }

  @Test
  void errorInOnePluginDoesNotAffectOthers() throws Exception {
    PluginJarHelper.createPluginJar(FailingOnLoadPlugin.class, pluginsDir);
    PluginJarHelper.createPluginJar(TestPlugin.class, pluginsDir);

    PluginManager manager = createManager();
    manager.loadPlugins();

    List<PluginDescriptor> descriptors = manager.getPlugins();
    assertEquals(2, descriptors.size());
    for (PluginDescriptor desc : descriptors) {
      if ("FailingOnLoadPlugin".equals(desc.getName())) {
        assertEquals(PluginState.ERROR, desc.getState());
      } else if ("TestPlugin".equals(desc.getName())) {
        assertEquals(PluginState.LOADED, desc.getState());
      }
    }
  }

  @Test
  void disablePluginsDoesNotThrowWhenNoPluginsLoaded() {
    PluginManager manager = createManager();
    manager.disablePlugins();
  }

  @Test
  void loadPluginsCreatesDataFolder() throws Exception {
    PluginJarHelper.createPluginJar(TestPlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();

    Path dataFolder = pluginsDataDir.resolve("TestPlugin");
    assertTrue(Files.isDirectory(dataFolder));
  }

  @Test
  void loadPluginsNonExistentDirectory() {
    PluginManager manager =
        new PluginManager(
            serviceRegistry,
            loggingService,
            Path.of("does-not-exist-12345"),
            pluginsDataDir,
            "0.1.0");
    List<PluginDescriptor> descriptors = manager.loadPlugins();
    assertTrue(descriptors.isEmpty());
  }

  @Test
  void loadEnableDisableWithEventBusSubscribesOnEnableCancelsOnDisable() throws Exception {
    EventBusImpl eventBus = new EventBusImpl(new TestLogger());
    serviceRegistry.register(EventBus.class, eventBus);

    PluginJarHelper.createPluginJar(SubscribeOnEnablePlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();
    assertTrue(handlerCount(eventBus) == 0, "no subscriptions before enable");

    manager.enablePlugins();
    assertTrue(handlerCount(eventBus) > 0, "plugin should have subscribed in onEnable");

    manager.disablePlugins();
    assertEquals(
        0, handlerCount(eventBus), "all plugin subscriptions should be cancelled after disable");
  }

  @Test
  void subscriptionsCancelledEvenWhenPluginOnDisableThrows() throws Exception {
    EventBusImpl eventBus = new EventBusImpl(new TestLogger());
    serviceRegistry.register(EventBus.class, eventBus);

    PluginJarHelper.createPluginJar(FailingOnDisablePlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();
    manager.enablePlugins();

    manager.disablePlugins();
    assertEquals(
        0,
        handlerCount(eventBus),
        "subscriptions should be cleaned up even after onDisable throws");
  }

  @Test
  void disablePluginsWithoutEventBusDoesNotThrow() throws Exception {
    PluginJarHelper.createPluginJar(TestPlugin.class, pluginsDir);
    PluginManager manager = createManager();
    manager.loadPlugins();
    manager.enablePlugins();
    manager.disablePlugins();

    assertTrue(manager.getPlugins().isEmpty());
  }

  @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
  private static int handlerCount(EventBusImpl bus) {
    try {
      Field field = EventBusImpl.class.getDeclaredField("handlers");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<Class<?>, List<?>> handlers = (Map<Class<?>, List<?>>) field.get(bus);
      // Count only entries that are not cancelled
      int count = 0;
      for (List<?> list : handlers.values()) {
        for (Object entry : list) {
          Field cancelledField = entry.getClass().getDeclaredField("cancelled");
          cancelledField.setAccessible(true);
          if (!((java.util.concurrent.atomic.AtomicBoolean) cancelledField.get(entry)).get()) {
            count++;
          }
        }
      }
      return count;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private PluginManager createManager() {
    return new PluginManager(serviceRegistry, loggingService, pluginsDir, pluginsDataDir, "0.1.0");
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
