package io.citadel.core.config;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CitadelConfigurationTest {

  @Test
  void loadWithBareFilenameDoesNotThrowNpe(@TempDir Path tempDir) {
    Path configFile = tempDir.resolve("bare.yml");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    assertDoesNotThrow(config::load);
    assertTrue(Files.exists(configFile));
  }

  @Test
  void generatesDefaultConfigOnFirstLoad(@TempDir Path tempDir) {
    Path configFile = tempDir.resolve("citadel.yml");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertTrue(Files.exists(configFile));
  }

  @Test
  void generatedDefaultConfigIsValid(@TempDir Path tempDir) {
    Path configFile = tempDir.resolve("citadel.yml");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertEquals("localhost", config.getString("networking.server.host"));
    assertEquals(25565, config.getInt("networking.server.port"));
    assertEquals("INFO", config.getString("logging.level"));
  }

  @Test
  void readsExistingConfigFile(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(
        configFile, "networking:\n  server:\n    host: my-server.com\n    port: 12345\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertEquals("my-server.com", config.getString("networking.server.host"));
    assertEquals(12345, config.getInt("networking.server.port"));
  }

  @Test
  void reloadReplacesValues(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "key: original\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertEquals("original", config.getString("key"));

    Files.writeString(configFile, "key: updated\n");
    config.reload();
    assertEquals("updated", config.getString("key"));
  }

  @Test
  void listenerNotifiedOnReload(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "key: value\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();

    AtomicBoolean notified = new AtomicBoolean(false);
    config.addListener(c -> notified.set(true));
    config.reload();
    assertTrue(notified.get());
  }

  @Test
  void listenerNotNotifiedWithoutReload(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "key: value\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();

    AtomicBoolean notified = new AtomicBoolean(false);
    config.addListener(c -> notified.set(true));
    assertFalse(notified.get());
  }

  @Test
  void removedListenerNotNotified(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "key: value\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();

    AtomicBoolean notified = new AtomicBoolean(false);
    ConfigurationListener listener = c -> notified.set(true);
    config.addListener(listener);
    config.removeListener(listener);
    config.reload();
    assertFalse(notified.get());
  }

  @Test
  void getPluginSectionReturnsPluginNamespace(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(
        configFile, "plugins:\n  map-art:\n    image_path: input.png\n    threads: 4\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();

    ConfigurationSection pluginSection = config.getPluginSection("map-art");
    assertEquals("input.png", pluginSection.getString("image_path"));
    assertEquals(4, pluginSection.getInt("threads"));
  }

  @Test
  void getPluginSectionReturnsEmptyForMissingPlugin(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "plugins: {}\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();

    ConfigurationSection pluginSection = config.getPluginSection("nonexistent");
    assertNotNull(pluginSection);
    assertTrue(pluginSection.getKeys().isEmpty());
  }

  @Test
  void containsReturnsCorrectly(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "existing: yes\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();

    assertTrue(config.contains("existing"));
    assertFalse(config.contains("nonexistent"));
  }

  @Test
  void getRootReturnsSameValues(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "key: value\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();

    assertEquals("value", config.getRoot().getString("key"));
  }

  @Test
  void concurrentReadsDuringReloadSeeConsistentSnapshot(@TempDir Path tempDir)
      throws IOException, InterruptedException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "key: original\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();

    AtomicBoolean readerSawInconsistent = new AtomicBoolean(false);
    Thread reader =
        new Thread(
            () -> {
              for (int i = 0; i < 1000; i++) {
                String val = config.getString("key");
                if (val == null || val.isEmpty()) {
                  readerSawInconsistent.set(true);
                }
              }
            });
    reader.start();

    for (int i = 0; i < 100; i++) {
      config.reload();
    }
    reader.join();
    assertFalse(readerSawInconsistent.get());
  }

  @Test
  void malformedFileThrowsException(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "invalid: [\nunclosed\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    assertThrows(io.citadel.api.service.ConfigurationException.class, config::load);
  }

  @Test
  void nestedSectionAccess(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "a:\n  b:\n    c:\n      d: deep\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertEquals("deep", config.getString("a.b.c.d"));
    assertEquals("deep", config.getSection("a.b.c").getString("d"));
  }

  @Test
  void getStringListFromConfig(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "items:\n  - a\n  - b\n  - c\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertEquals(3, config.getStringList("items").size());
    assertFalse(config.getList("items").isEmpty());
  }

  @Test
  void getKeysReturnsRootKeys(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "a: 1\nb: 2\nc: 3\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertTrue(config.getKeys().contains("a"));
    assertTrue(config.getKeys().contains("b"));
    assertTrue(config.getKeys().contains("c"));
  }

  @Test
  void configFileCreatedInCorrectLocation(@TempDir Path tempDir) {
    Path configFile = tempDir.resolve("citadel.yml");
    assertFalse(Files.exists(configFile));
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertTrue(Files.exists(configFile));
  }

  @Test
  void reloadPreservesValuesUntilNewFileWritten(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("citadel.yml");
    Files.writeString(configFile, "key: before\n");
    CitadelConfiguration config = new CitadelConfiguration(configFile);
    config.load();
    assertEquals("before", config.getString("key"));

    Files.writeString(configFile, "key: after\n");
    assertEquals("before", config.getString("key"));

    config.reload();
    assertEquals("after", config.getString("key"));
  }
}
