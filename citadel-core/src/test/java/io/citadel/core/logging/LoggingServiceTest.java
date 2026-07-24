package io.citadel.core.logging;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.service.Logger;
import org.junit.jupiter.api.Test;

class LoggingServiceTest {

  @Test
  void getRootLoggerReturnsNonNull() {
    LoggingService service = new LoggingService();
    assertNotNull(service.getRootLogger());
  }

  @Test
  void getPluginLoggerReturnsNonNull() {
    LoggingService service = new LoggingService();
    Logger logger = service.getPluginLogger("map-art");
    assertNotNull(logger);
    logger.info("plugin message");
  }

  @Test
  void getPluginLoggerProducesNamespacedLogger() {
    LoggingService service = new LoggingService();
    Logger mapArt = service.getPluginLogger("map-art");
    Logger discord = service.getPluginLogger("discord");
    assertNotNull(mapArt);
    assertNotNull(discord);
    mapArt.info("MapArt says hello");
    discord.info("Discord says hello");
  }

  @Test
  void getLoggerReturnsSameInstanceForSameName() {
    LoggingService service = new LoggingService();
    Logger a = service.getLogger("test.logger");
    Logger b = service.getLogger("test.logger");
    assertSame(a, b);
  }

  @Test
  void getLoggerReturnsDifferentInstanceForDifferentNames() {
    LoggingService service = new LoggingService();
    Logger a = service.getLogger("test.logger.a");
    Logger b = service.getLogger("test.logger.b");
    assertNotSame(a, b);
  }

  @Test
  void getPluginLoggerReturnsSameInstanceForSamePlugin() {
    LoggingService service = new LoggingService();
    Logger a = service.getPluginLogger("map-art");
    Logger b = service.getPluginLogger("map-art");
    assertSame(a, b);
  }

  @Test
  void getPluginLoggerDifferentForDifferentPlugins() {
    LoggingService service = new LoggingService();
    Logger a = service.getPluginLogger("map-art");
    Logger b = service.getPluginLogger("discord");
    assertNotSame(a, b);
  }

  @Test
  void shutdownDoesNotThrow() {
    LoggingService service = new LoggingService();
    service.shutdown();
  }

  @Test
  void loggerWorksBeforeInitialization() {
    LoggingService service = new LoggingService();
    Logger logger = service.getRootLogger();
    assertNotNull(logger);
    logger.info("this should not throw");
  }

  @Test
  void concurrentLoggerAccessDoesNotThrow() throws InterruptedException {
    LoggingService service = new LoggingService();
    Thread t1 =
        new Thread(
            () -> {
              for (int i = 0; i < 100; i++) {
                service.getPluginLogger("plugin-" + i).info("thread 1");
              }
            });
    Thread t2 =
        new Thread(
            () -> {
              for (int i = 0; i < 100; i++) {
                service.getPluginLogger("plugin-" + i).info("thread 2");
              }
            });
    t1.start();
    t2.start();
    t1.join();
    t2.join();
  }
}
