package io.citadel.core.logging;

import io.citadel.api.service.Logger;
import org.slf4j.MDC;

/**
 * Implementation of {@link Logger} that wraps an SLF4J logger.
 *
 * <p>Account-aware methods set the {@code account} key in the SLF4J MDC before delegating to the
 * SLF4J logger and remove it afterward. The MDC value is included in formatted log output via the
 * pattern layout.
 *
 * <p>Thread safety: thread-safe. SLF4J logger instances are thread-safe. MDC operations use {@link
 * MDC#put(String, String)} and {@link MDC#remove(String)} which are thread-local and do not require
 * external synchronization.
 */
final class Slf4jLogger implements Logger {

  private static final String ACCOUNT_MDC_KEY = "account";

  private final org.slf4j.Logger logger;

  Slf4jLogger(org.slf4j.Logger logger) {
    this.logger = logger;
  }

  // -- Level checks --

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  // -- Trace --

  @Override
  public void trace(String message) {
    logger.trace(message);
  }

  @Override
  public void trace(String format, Object... args) {
    logger.trace(format, args);
  }

  @Override
  public void trace(String message, Throwable throwable) {
    logger.trace(message, throwable);
  }

  @Override
  public void trace(String accountName, String message) {
    putAccount(accountName);
    logger.trace(message);
    removeAccount();
  }

  @Override
  public void trace(String accountName, String format, Object... args) {
    putAccount(accountName);
    logger.trace(format, args);
    removeAccount();
  }

  @Override
  public void trace(String accountName, String message, Throwable throwable) {
    putAccount(accountName);
    logger.trace(message, throwable);
    removeAccount();
  }

  // -- Debug --

  @Override
  public void debug(String message) {
    logger.debug(message);
  }

  @Override
  public void debug(String format, Object... args) {
    logger.debug(format, args);
  }

  @Override
  public void debug(String message, Throwable throwable) {
    logger.debug(message, throwable);
  }

  @Override
  public void debug(String accountName, String message) {
    putAccount(accountName);
    logger.debug(message);
    removeAccount();
  }

  @Override
  public void debug(String accountName, String format, Object... args) {
    putAccount(accountName);
    logger.debug(format, args);
    removeAccount();
  }

  @Override
  public void debug(String accountName, String message, Throwable throwable) {
    putAccount(accountName);
    logger.debug(message, throwable);
    removeAccount();
  }

  // -- Info --

  @Override
  public void info(String message) {
    logger.info(message);
  }

  @Override
  public void info(String format, Object... args) {
    logger.info(format, args);
  }

  @Override
  public void info(String message, Throwable throwable) {
    logger.info(message, throwable);
  }

  @Override
  public void info(String accountName, String message) {
    putAccount(accountName);
    logger.info(message);
    removeAccount();
  }

  @Override
  public void info(String accountName, String format, Object... args) {
    putAccount(accountName);
    logger.info(format, args);
    removeAccount();
  }

  @Override
  public void info(String accountName, String message, Throwable throwable) {
    putAccount(accountName);
    logger.info(message, throwable);
    removeAccount();
  }

  // -- Warn --

  @Override
  public void warn(String message) {
    logger.warn(message);
  }

  @Override
  public void warn(String format, Object... args) {
    logger.warn(format, args);
  }

  @Override
  public void warn(String message, Throwable throwable) {
    logger.warn(message, throwable);
  }

  @Override
  public void warn(String accountName, String message) {
    putAccount(accountName);
    logger.warn(message);
    removeAccount();
  }

  @Override
  public void warn(String accountName, String format, Object... args) {
    putAccount(accountName);
    logger.warn(format, args);
    removeAccount();
  }

  @Override
  public void warn(String accountName, String message, Throwable throwable) {
    putAccount(accountName);
    logger.warn(message, throwable);
    removeAccount();
  }

  // -- Error --

  @Override
  public void error(String message) {
    logger.error(message);
  }

  @Override
  public void error(String format, Object... args) {
    logger.error(format, args);
  }

  @Override
  public void error(String message, Throwable throwable) {
    logger.error(message, throwable);
  }

  @Override
  public void error(String accountName, String message) {
    putAccount(accountName);
    logger.error(message);
    removeAccount();
  }

  @Override
  public void error(String accountName, String format, Object... args) {
    putAccount(accountName);
    logger.error(format, args);
    removeAccount();
  }

  @Override
  public void error(String accountName, String message, Throwable throwable) {
    putAccount(accountName);
    logger.error(message, throwable);
    removeAccount();
  }

  // -- MDC helpers --

  private static void putAccount(String accountName) {
    MDC.put(ACCOUNT_MDC_KEY, accountName);
  }

  private static void removeAccount() {
    MDC.remove(ACCOUNT_MDC_KEY);
  }
}
