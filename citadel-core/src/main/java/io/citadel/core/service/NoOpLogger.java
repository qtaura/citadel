package io.citadel.core.service;

import io.citadel.api.service.Logger;

/**
 * Placeholder implementation of {@link Logger} used before the logging service is initialized.
 *
 * @deprecated replaced by {@code Slf4jLogger} in Milestone 6
 */
@Deprecated
public final class NoOpLogger implements Logger {

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
    return false;
  }

  @Override
  public boolean isErrorEnabled() {
    return false;
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
