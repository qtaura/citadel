package io.citadel.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import io.citadel.api.service.ConfigurationSection;
import java.nio.file.Path;
import org.slf4j.LoggerFactory;

/**
 * Programmatically configures Logback based on the {@code logging} section of Citadel
 * configuration.
 *
 * <p>Configuration is applied once at startup and can be reapplied via {@link
 * #reconfigure(ConfigurationSection)} on configuration reload. Only the Logback {@link
 * LoggerContext} and related classes are used — callers should never depend on Logback APIs
 * directly.
 */
final class LogbackConfigurer {

  private static final String CONSOLE_PATTERN = "[%d{HH:mm:ss}] %-5level %msg%X{account}%n";
  private static final String FILE_PATTERN =
      "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%X{account}%n";

  private LogbackConfigurer() {}

  /** Configures Logback for the first time with default or configured settings. */
  static void initialize(ConfigurationSection loggingConfig) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    rootLogger.detachAndStopAllAppenders();

    Level rootLevel = parseLevel(loggingConfig.getString("level", "INFO"));
    rootLogger.setLevel(rootLevel);

    ConsoleAppender<ILoggingEvent> consoleAppender = createConsoleAppender(context);
    consoleAppender.start();
    rootLogger.addAppender(consoleAppender);

    ConfigurationSection fileSection = loggingConfig.getSection("file");
    if (fileSection.getBoolean("enabled", true)) {
      RollingFileAppender<ILoggingEvent> fileAppender = createFileAppender(context, fileSection);
      fileAppender.start();
      rootLogger.addAppender(fileAppender);
    }
  }

  /** Reconfigures Logback on configuration reload. */
  static void reconfigure(ConfigurationSection loggingConfig) {
    initialize(loggingConfig);
  }

  /** Shuts down the Logback context, flushing all pending log events. */
  static void shutdown() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAndStopAllAppenders();
  }

  private static ConsoleAppender<ILoggingEvent> createConsoleAppender(LoggerContext context) {
    PatternLayoutEncoder encoder = createEncoder(context, CONSOLE_PATTERN);
    ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    appender.setContext(context);
    appender.setName("CONSOLE");
    appender.setEncoder(encoder);
    return appender;
  }

  private static RollingFileAppender<ILoggingEvent> createFileAppender(
      LoggerContext context, ConfigurationSection fileSection) {
    String logPath = fileSection.getString("path", "logs/citadel.log");
    String maxSize = fileSection.getString("max_size", "10MB");
    int maxHistory = fileSection.getInt("max_history", 7);

    PatternLayoutEncoder encoder = createEncoder(context, FILE_PATTERN);

    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
    appender.setContext(context);
    appender.setName("FILE");
    appender.setEncoder(encoder);
    appender.setFile(logPath);

    Path logFile = Path.of(logPath);
    String logBaseName = logFile.getFileName().toString();
    String logDir = logFile.getParent() != null ? logFile.getParent().toString() : ".";
    int lastDot = logBaseName.lastIndexOf('.');
    String nameBody = lastDot > 0 ? logBaseName.substring(0, lastDot) : logBaseName;

    SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy =
        new SizeAndTimeBasedRollingPolicy<>();
    rollingPolicy.setContext(context);
    rollingPolicy.setFileNamePattern(logDir + "/" + nameBody + ".%d{yyyy-MM-dd}.%i.log");
    rollingPolicy.setMaxHistory(maxHistory);
    rollingPolicy.setMaxFileSize(FileSize.valueOf(maxSize));
    rollingPolicy.setParent(appender);
    rollingPolicy.start();

    appender.setRollingPolicy(rollingPolicy);

    return appender;
  }

  private static PatternLayoutEncoder createEncoder(LoggerContext context, String pattern) {
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern(pattern);
    return encoder;
  }

  private static Level parseLevel(String levelName) {
    switch (levelName.toUpperCase()) {
      case "TRACE":
        return Level.TRACE;
      case "DEBUG":
        return Level.DEBUG;
      case "INFO":
        return Level.INFO;
      case "WARN":
        return Level.WARN;
      case "ERROR":
        return Level.ERROR;
      default:
        return Level.INFO;
    }
  }
}
