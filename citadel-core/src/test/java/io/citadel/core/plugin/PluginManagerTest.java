package io.citadel.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.core.bootstrap.ServiceRegistry;
import io.citadel.core.logging.LoggingService;
import io.citadel.core.plugin.testplugin.FailingOnDisablePlugin;
import io.citadel.core.plugin.testplugin.FailingOnEnablePlugin;
import io.citadel.core.plugin.testplugin.FailingOnLoadPlugin;
import io.citadel.core.plugin.testplugin.TestPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

  private PluginManager createManager() {
    return new PluginManager(serviceRegistry, loggingService, pluginsDir, pluginsDataDir, "0.1.0");
  }
}
