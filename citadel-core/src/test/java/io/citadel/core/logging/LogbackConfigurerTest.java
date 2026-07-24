package io.citadel.core.logging;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.citadel.api.service.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class LogbackConfigurerTest {

  @AfterEach
  void tearDown() {
    LogbackConfigurer.shutdown();
  }

  @Test
  void initializeDoesNotThrow() {
    assertDoesNotThrow(() -> LogbackConfigurer.initialize(new TestLoggingConfig()));
  }

  @Test
  void reconfigureDoesNotThrow() {
    LogbackConfigurer.initialize(new TestLoggingConfig());
    assertDoesNotThrow(() -> LogbackConfigurer.reconfigure(new TestLoggingConfig()));
  }

  @Test
  void shutdownDoesNotThrow() {
    LogbackConfigurer.initialize(new TestLoggingConfig());
    assertDoesNotThrow(() -> LogbackConfigurer.shutdown());
  }

  @Test
  void consoleLoggingWorksAfterInitialize() {
    LogbackConfigurer.initialize(new TestLoggingConfig());
    Logger logger = new Slf4jLogger(LoggerFactory.getLogger("test.console"));
    assertDoesNotThrow(() -> logger.info("Console test"));
  }

  @Test
  void levelFilteringWorks() {
    TestLoggingConfig config = new TestLoggingConfig();
    config.level = "ERROR";
    LogbackConfigurer.initialize(config);
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    assertEquals(Level.ERROR, context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel());
    ch.qos.logback.classic.Logger childLogger =
        context.getLogger("test.filter." + System.nanoTime());
    assertEquals(Level.ERROR, childLogger.getEffectiveLevel());
    Logger logger = new Slf4jLogger(childLogger);
    assertDoesNotThrow(() -> logger.info("should be suppressed"));
    assertDoesNotThrow(() -> logger.error("should appear"));
  }

  @Test
  void reconfigureChangesLevel() {
    TestLoggingConfig config = new TestLoggingConfig();
    config.level = "DEBUG";
    LogbackConfigurer.initialize(config);
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    assertEquals(Level.DEBUG, context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel());
    config.level = "ERROR";
    LogbackConfigurer.reconfigure(config);
    assertEquals(Level.ERROR, context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel());
  }

  @Test
  void defaultConfigUsesInfoLevel() {
    TestLoggingConfig config = new TestLoggingConfig();
    config.level = null;
    LogbackConfigurer.initialize(config);
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    assertEquals(Level.INFO, context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel());
  }

  @Test
  void unknownLevelDefaultsToInfo() {
    TestLoggingConfig config = new TestLoggingConfig();
    config.level = "INVALID_LEVEL";
    LogbackConfigurer.initialize(config);
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    assertEquals(Level.INFO, context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel());
  }

  @Test
  void fileLoggingDisabledDoesNotCreateFile(@TempDir Path tempDir) {
    String logPath = tempDir.resolve("no-file.log").toString();
    TestLoggingConfig config = new TestLoggingConfig();
    config.fileEnabled = false;
    config.filePath = logPath;
    LogbackConfigurer.initialize(config);
    assertDoesNotThrow(() -> new Slf4jLogger(LoggerFactory.getLogger("test")).info("no file test"));
    LogbackConfigurer.shutdown();
    assertFalse(Files.exists(Path.of(logPath)));
  }

  @Test
  void rotationConfigurationIsAccepted(@TempDir Path tempDir) throws IOException {
    String logPath = tempDir.resolve("rotation.log").toString();
    TestLoggingConfig config = new TestLoggingConfig();
    config.fileEnabled = true;
    config.filePath = logPath;
    config.maxSize = "10KB";
    config.maxHistory = 3;
    assertDoesNotThrow(() -> LogbackConfigurer.initialize(config));
  }

  private static final class TestLoggingConfig
      implements io.citadel.api.service.ConfigurationSection {

    String level = "INFO";
    boolean fileEnabled;
    String filePath = "logs/test.log";
    String maxSize = "10MB";
    int maxHistory = 7;

    @Override
    public String getString(String path) {
      if ("level".equals(path)) {
        if (level == null) {
          throw new RuntimeException("missing");
        }
        return level;
      }
      if ("file.path".equals(path)) {
        return filePath;
      }
      if ("file.max_size".equals(path)) {
        return maxSize;
      }
      throw new RuntimeException("Missing: " + path);
    }

    @Override
    public String getString(String path, String defaultValue) {
      if ("level".equals(path)) {
        return level != null ? level : defaultValue;
      }
      if ("file.path".equals(path)) {
        return filePath;
      }
      if ("file.max_size".equals(path)) {
        return maxSize;
      }
      return defaultValue;
    }

    @Override
    public boolean getBoolean(String path) {
      if ("file.enabled".equals(path)) {
        return fileEnabled;
      }
      throw new RuntimeException("Missing: " + path);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
      if ("file.enabled".equals(path)) {
        return fileEnabled;
      }
      return defaultValue;
    }

    @Override
    public int getInt(String path) {
      if ("file.max_history".equals(path)) {
        return maxHistory;
      }
      throw new RuntimeException("Missing: " + path);
    }

    @Override
    public int getInt(String path, int defaultValue) {
      if ("file.max_history".equals(path)) {
        return maxHistory;
      }
      return defaultValue;
    }

    @Override
    public long getLong(String path) {
      throw new RuntimeException("Unexpected: " + path);
    }

    @Override
    public long getLong(String path, long defaultValue) {
      return defaultValue;
    }

    @Override
    public double getDouble(String path) {
      throw new RuntimeException("Unexpected: " + path);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
      return defaultValue;
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
      throw new RuntimeException("Unexpected: " + path);
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
      return defaultValue;
    }

    @Override
    public java.util.List<String> getStringList(String path) {
      throw new RuntimeException("Unexpected: " + path);
    }

    @Override
    public <T> java.util.List<T> getList(String path) {
      throw new RuntimeException("Unexpected: " + path);
    }

    @Override
    public io.citadel.api.service.ConfigurationSection getSection(String path) {
      if ("file".equals(path)) {
        return new FileSection();
      }
      throw new RuntimeException("Unexpected: " + path);
    }

    @Override
    public boolean contains(String path) {
      return "level".equals(path)
          || "file.enabled".equals(path)
          || "file.path".equals(path)
          || "file.max_size".equals(path)
          || "file.max_history".equals(path);
    }

    @Override
    public java.util.Set<String> getKeys() {
      return java.util.Set.of("level", "file");
    }

    private final class FileSection implements io.citadel.api.service.ConfigurationSection {

      @Override
      public String getString(String path) {
        if ("path".equals(path)) {
          return filePath;
        }
        if ("max_size".equals(path)) {
          return maxSize;
        }
        throw new RuntimeException("Missing: " + path);
      }

      @Override
      public String getString(String path, String defaultValue) {
        if ("path".equals(path)) {
          return filePath;
        }
        if ("max_size".equals(path)) {
          return maxSize;
        }
        return defaultValue;
      }

      @Override
      public boolean getBoolean(String path) {
        if ("enabled".equals(path)) {
          return fileEnabled;
        }
        throw new RuntimeException("Missing: " + path);
      }

      @Override
      public boolean getBoolean(String path, boolean defaultValue) {
        if ("enabled".equals(path)) {
          return fileEnabled;
        }
        return defaultValue;
      }

      @Override
      public int getInt(String path) {
        if ("max_history".equals(path)) {
          return maxHistory;
        }
        throw new RuntimeException("Missing: " + path);
      }

      @Override
      public int getInt(String path, int defaultValue) {
        if ("max_history".equals(path)) {
          return maxHistory;
        }
        return defaultValue;
      }

      @Override
      public long getLong(String path) {
        throw new RuntimeException("Unexpected: " + path);
      }

      @Override
      public long getLong(String path, long defaultValue) {
        return defaultValue;
      }

      @Override
      public double getDouble(String path) {
        throw new RuntimeException("Unexpected: " + path);
      }

      @Override
      public double getDouble(String path, double defaultValue) {
        return defaultValue;
      }

      @Override
      public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
        throw new RuntimeException("Unexpected: " + path);
      }

      @Override
      public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
        return defaultValue;
      }

      @Override
      public java.util.List<String> getStringList(String path) {
        throw new RuntimeException("Unexpected: " + path);
      }

      @Override
      public <T> java.util.List<T> getList(String path) {
        throw new RuntimeException("Unexpected: " + path);
      }

      @Override
      public io.citadel.api.service.ConfigurationSection getSection(String path) {
        throw new RuntimeException("Unexpected: " + path);
      }

      @Override
      public boolean contains(String path) {
        return "enabled".equals(path)
            || "path".equals(path)
            || "max_size".equals(path)
            || "max_history".equals(path);
      }

      @Override
      public java.util.Set<String> getKeys() {
        return java.util.Set.of("enabled", "path", "max_size", "max_history");
      }
    }
  }
}
