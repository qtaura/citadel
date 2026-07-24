package io.citadel.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.core.plugin.testplugin.IncompatibleApiPlugin;
import io.citadel.core.plugin.testplugin.NoAnnotationPlugin;
import io.citadel.core.plugin.testplugin.NotAPlugin;
import io.citadel.core.plugin.testplugin.TestPlugin;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginScannerTest {

  @Test
  void scanWithNoPluginsDirectoryReturnsEmptyList() {
    PluginScanner scanner = new PluginScanner(Path.of("does-not-exist-12345"));
    List<PluginDescriptor> result = scanner.scan("0.1.0");
    assertTrue(result.isEmpty());
  }

  @Test
  void scanWithEmptyDirectoryReturnsEmptyList(@TempDir Path pluginsDir) {
    PluginScanner scanner = new PluginScanner(pluginsDir);
    List<PluginDescriptor> result = scanner.scan("0.1.0");
    assertTrue(result.isEmpty());
  }

  @Test
  void scanWithValidPluginReturnsDescriptor(@TempDir Path pluginsDir) throws Exception {
    PluginJarHelper.createPluginJar(TestPlugin.class, pluginsDir);
    PluginScanner scanner = new PluginScanner(pluginsDir);
    List<PluginDescriptor> result = scanner.scan("0.1.0");
    assertEquals(1, result.size());
    PluginDescriptor desc = result.get(0);
    assertEquals("TestPlugin", desc.getName());
    assertEquals("1.0.0", desc.getVersion());
    assertEquals("0.1.0", desc.getApiVersion());
    assertEquals(TestPlugin.class.getName(), desc.getMainClassName());
  }

  @Test
  void scanSkipsIncompatibleApiVersion(@TempDir Path pluginsDir) throws Exception {
    PluginJarHelper.createPluginJar(IncompatibleApiPlugin.class, pluginsDir);
    PluginScanner scanner = new PluginScanner(pluginsDir);
    List<PluginDescriptor> result = scanner.scan("0.1.0");
    assertTrue(result.isEmpty());
  }

  @Test
  void scanSkipsClassWithoutPluginInterface(@TempDir Path pluginsDir) throws Exception {
    PluginJarHelper.createPluginJar(NotAPlugin.class, pluginsDir);
    PluginScanner scanner = new PluginScanner(pluginsDir);
    List<PluginDescriptor> result = scanner.scan("0.1.0");
    assertTrue(result.isEmpty());
  }

  @Test
  void scanSkipsClassWithoutAnnotation(@TempDir Path pluginsDir) throws Exception {
    PluginJarHelper.createPluginJar(NoAnnotationPlugin.class, pluginsDir);
    PluginScanner scanner = new PluginScanner(pluginsDir);
    List<PluginDescriptor> result = scanner.scan("0.1.0");
    assertTrue(result.isEmpty());
  }

  @Test
  void scanWithMultipleJarsReturnsOnlyValidPlugins(@TempDir Path pluginsDir) throws Exception {
    PluginJarHelper.createPluginJar(TestPlugin.class, pluginsDir);
    PluginJarHelper.createPluginJar(NoAnnotationPlugin.class, pluginsDir);
    PluginJarHelper.createPluginJar(IncompatibleApiPlugin.class, pluginsDir);

    PluginScanner scanner = new PluginScanner(pluginsDir);
    List<PluginDescriptor> result = scanner.scan("0.1.0");
    assertEquals(1, result.size());
    assertEquals("TestPlugin", result.get(0).getName());
  }

  @Test
  void isApiCompatibleSameMajorReturnsTrue() {
    assertTrue(PluginScanner.isApiCompatible("0.1.0", "0.1.0"));
    assertTrue(PluginScanner.isApiCompatible("0.2.0", "0.1.0"));
    assertTrue(PluginScanner.isApiCompatible("0.1.0", "0.2.0"));
  }

  @Test
  void isApiCompatibleDifferentMajorReturnsFalse() {
    assertFalse(PluginScanner.isApiCompatible("1.0.0", "0.1.0"));
    assertFalse(PluginScanner.isApiCompatible("0.1.0", "1.0.0"));
    assertFalse(PluginScanner.isApiCompatible("2.0.0", "1.0.0"));
  }

  @Test
  void isApiCompatibleHandlesSnapshot() {
    assertTrue(PluginScanner.isApiCompatible("0.1.0", "0.1.0-SNAPSHOT"));
    assertTrue(PluginScanner.isApiCompatible("0.2.0-SNAPSHOT", "0.1.0"));
  }

  @Test
  void isApiCompatibleInvalidVersionReturnsFalse() {
    assertFalse(PluginScanner.isApiCompatible("invalid", "0.1.0"));
    assertFalse(PluginScanner.isApiCompatible("0.1.0", "invalid"));
  }
}
