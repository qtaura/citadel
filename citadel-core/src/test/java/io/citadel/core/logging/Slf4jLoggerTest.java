package io.citadel.core.logging;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.service.Logger;
import org.junit.jupiter.api.Test;

class Slf4jLoggerTest {

  @Test
  void rootLoggerIsNeverNull() {
    Logger logger = createLogger("test");
    assertNotNull(logger);
  }

  @Test
  void infoLoggingDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.info("Hello, world!");
  }

  @Test
  void infoWithFormatDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.info("Hello, {}!", "world");
  }

  @Test
  void infoWithThrowableDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.info("Something happened", new RuntimeException("test"));
  }

  @Test
  void debugLoggingDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.debug("debug message");
  }

  @Test
  void warnLoggingDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.warn("warn message");
  }

  @Test
  void errorLoggingDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.error("error message");
  }

  @Test
  void traceLoggingDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.trace("trace message");
  }

  @Test
  void accountContextDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.info("Builder01", "Account connected");
  }

  @Test
  void accountContextWithFormatDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.info("Builder01", "Account {} connected", "Builder01");
  }

  @Test
  void accountContextWithThrowableDoesNotThrow() {
    Logger logger = createLogger("test");
    logger.info("Builder01", "Account error", new RuntimeException("test"));
  }

  @Test
  void multipleAccountCallsAreIndependent() {
    Logger logger = createLogger("test");
    logger.info("account1", "First message");
    logger.info("account2", "Second message");
  }

  @Test
  void allLevelsWithAccountContextWork() {
    Logger logger = createLogger("test");
    logger.trace("acc1", "trace");
    logger.debug("acc1", "debug");
    logger.info("acc1", "info");
    logger.warn("acc1", "warn");
    logger.error("acc1", "error");
  }

  @Test
  void allLevelsWithThrowableWork() {
    Logger logger = createLogger("test");
    RuntimeException ex = new RuntimeException("test");
    logger.trace("trace", ex);
    logger.debug("debug", ex);
    logger.info("info", ex);
    logger.warn("warn", ex);
    logger.error("error", ex);
  }

  @Test
  void allLevelsWithAccountAndThrowableWork() {
    Logger logger = createLogger("test");
    RuntimeException ex = new RuntimeException("test");
    logger.trace("acc", "trace", ex);
    logger.debug("acc", "debug", ex);
    logger.info("acc", "info", ex);
    logger.warn("acc", "warn", ex);
    logger.error("acc", "error", ex);
  }

  @Test
  void formatWithMultipleArgsWorks() {
    Logger logger = createLogger("test");
    logger.info("{} + {} = {}", 1, 2, 3);
  }

  private static Logger createLogger(String name) {
    return new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(name));
  }
}
