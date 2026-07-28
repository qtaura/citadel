package io.citadel.core.bot;

import io.citadel.api.account.Account;
import io.citadel.api.auth.Session;
import io.citadel.api.bot.Bot;
import io.citadel.api.bot.BotState;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.bot.BotFailedEvent;
import io.citadel.api.event.bot.BotStartedEvent;
import io.citadel.api.event.bot.BotStartingEvent;
import io.citadel.api.event.bot.BotStoppedEvent;
import io.citadel.api.event.bot.BotStoppingEvent;
import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.Logger;
import io.citadel.core.auth.AuthenticationService;
import io.citadel.core.net.Connection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("PMD.ExceptionAsFlowControl")
public final class BotImpl implements Bot {

  private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
  private static final int DEFAULT_READ_TIMEOUT = 30000;

  private final UUID botId;
  private final String accountId;
  private final AccountManager accountManager;
  private final AuthenticationService authenticationService;
  private final EventBus eventBus;
  private final Logger logger;
  private final int connectTimeout;
  private final int readTimeout;
  private final String serverHost;
  private final int serverPort;

  private final AtomicReference<BotState> state;
  private final Object lock;

  private volatile Account account;
  private volatile Connection connection;
  private volatile Session session;
  private CompletableFuture<Void> startFuture;
  private CompletableFuture<Void> stopFuture;

  // Visible for testing; when non-null, startPipeline awaits this latch before proceeding
  volatile CountDownLatch startBlocker;

  // Visible for testing; when non-null, stopPipeline awaits this latch before proceeding
  volatile CountDownLatch stopBlocker;

  public BotImpl(
      String accountId,
      AccountManager accountManager,
      AuthenticationService authenticationService,
      EventBus eventBus,
      Logger logger,
      Configuration config) {
    this.botId = UUID.randomUUID();
    this.accountId = Objects.requireNonNull(accountId, "accountId");
    this.accountManager = Objects.requireNonNull(accountManager, "accountManager");
    this.authenticationService = authenticationService;
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    Objects.requireNonNull(config, "config");
    this.connectTimeout = config.getInt("networking.connect_timeout", DEFAULT_CONNECT_TIMEOUT);
    this.readTimeout = config.getInt("networking.read_timeout", DEFAULT_READ_TIMEOUT);
    this.serverHost = config.getString("networking.server.host", "localhost");
    this.serverPort = config.getInt("networking.server.port", 25565);
    this.state = new AtomicReference<>(BotState.CREATED);
    this.lock = new Object();
  }

  BotImpl(
      String accountId,
      AccountManager accountManager,
      AuthenticationService authenticationService,
      EventBus eventBus,
      Logger logger,
      int connectTimeout,
      int readTimeout,
      String serverHost,
      int serverPort) {
    this.botId = UUID.randomUUID();
    this.accountId = Objects.requireNonNull(accountId, "accountId");
    this.accountManager = Objects.requireNonNull(accountManager, "accountManager");
    this.authenticationService = authenticationService;
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.serverHost = Objects.requireNonNull(serverHost, "serverHost");
    this.serverPort = serverPort;
    this.state = new AtomicReference<>(BotState.CREATED);
    this.lock = new Object();
  }

  @Override
  public UUID getBotId() {
    return botId;
  }

  @Override
  public Account getAccount() {
    return account;
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public Session getSession() {
    return session;
  }

  @Override
  public BotState getState() {
    return state.get();
  }

  @Override
  public boolean isRunning() {
    return state.get() == BotState.RUNNING;
  }

  @Override
  public CompletableFuture<Void> start() {
    synchronized (lock) {
      BotState current = state.get();
      if (current == BotState.RUNNING) {
        return CompletableFuture.completedFuture(null);
      }
      if (current == BotState.STARTING && startFuture != null && !startFuture.isDone()) {
        return startFuture;
      }
      if (current != BotState.CREATED && current != BotState.STOPPED) {
        return CompletableFuture.failedFuture(
            new IllegalStateException("Cannot start from state: " + current));
      }
      state.set(BotState.STARTING);
      CompletableFuture<Void> future = new CompletableFuture<>();
      startFuture = future;
      eventBus.publishAsync(new BotStartingEvent(botId, accountId));
      Thread.startVirtualThread(() -> startPipeline(future));
      return future;
    }
  }

  @Override
  public CompletableFuture<Void> stop() {
    synchronized (lock) {
      BotState current = state.get();
      if (current == BotState.STOPPED || current == BotState.CREATED) {
        return CompletableFuture.completedFuture(null);
      }
      if (current == BotState.FAILED) {
        return CompletableFuture.completedFuture(null);
      }
      if (current == BotState.STOPPING && stopFuture != null && !stopFuture.isDone()) {
        return stopFuture;
      }
      state.set(BotState.STOPPING);
      CompletableFuture<Void> future = new CompletableFuture<>();
      stopFuture = future;
      eventBus.publishAsync(new BotStoppingEvent(botId, accountId));
      Thread.startVirtualThread(() -> stopPipeline(future));
      return future;
    }
  }

  @Override
  public CompletableFuture<Void> restart() {
    return stop().thenCompose(v -> start());
  }

  private void startPipeline(CompletableFuture<Void> future) {
    try {
      if (startBlocker != null) {
        startBlocker.await();
      }
      account = accountManager.get(accountId);
      if (account == null) {
        throw new IllegalStateException("Account not found: " + accountId);
      }
      if (!transitionTo(BotState.CONNECTING)) {
        return;
      }
      connection =
          new Connection(serverHost, serverPort, connectTimeout, readTimeout, eventBus, logger);
      connection.connect();
      if (!transitionTo(BotState.AUTHENTICATING)) {
        return;
      }
      session = authenticationService.login(connection, account);
      if (!transitionTo(BotState.RUNNING)) {
        return;
      }
      logger.info(
          "Bot {} started (account={}, server={}:{})", botId, accountId, serverHost, serverPort);
      eventBus.publishAsync(new BotStartedEvent(botId, accountId, serverHost, serverPort));
      future.complete(null);
    } catch (Exception e) {
      cleanup();
      state.set(BotState.FAILED);
      logger.error("Bot {} failed: {}", botId, e.getMessage());
      eventBus.publishAsync(new BotFailedEvent(botId, accountId, e.getMessage()));
      future.completeExceptionally(e);
    }
  }

  private void stopPipeline(CompletableFuture<Void> future) {
    try {
      if (stopBlocker != null) {
        stopBlocker.await();
      }
      cleanup();
      state.set(BotState.STOPPED);
      logger.info("Bot {} stopped", botId);
      eventBus.publishAsync(new BotStoppedEvent(botId, accountId));
      future.complete(null);
    } catch (Exception e) {
      state.set(BotState.FAILED);
      eventBus.publishAsync(new BotFailedEvent(botId, accountId, e.getMessage()));
      future.completeExceptionally(e);
    }
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private boolean transitionTo(BotState target) {
    while (true) {
      BotState current = state.get();
      if (current == BotState.STOPPING || current == BotState.STOPPED) {
        cleanup();
        state.set(BotState.STOPPED);
        logger.info("Bot {} startup aborted", botId);
        eventBus.publishAsync(new BotStoppedEvent(botId, accountId));
        startFuture.completeExceptionally(new IllegalStateException("Startup aborted by stop"));
        return false;
      }
      if (state.compareAndSet(current, target)) {
        return true;
      }
    }
  }

  private void cleanup() {
    if (connection != null) {
      try {
        connection.close();
      } catch (Exception ignored) {
      }
      connection = null;
    }
    session = null;
  }

  // Visible for testing
  void forceState(BotState target) {
    synchronized (lock) {
      state.set(target);
    }
  }
}
