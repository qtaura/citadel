package io.citadel.core.bot;

import io.citadel.api.account.Account;
import io.citadel.api.auth.Session;
import io.citadel.api.bot.Bot;
import io.citadel.api.bot.BotState;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.bot.BotFailedEvent;
import io.citadel.api.event.bot.BotReconnectFailedEvent;
import io.citadel.api.event.bot.BotReconnectStartedEvent;
import io.citadel.api.event.bot.BotReconnectSucceededEvent;
import io.citadel.api.event.bot.BotStartedEvent;
import io.citadel.api.event.bot.BotStartingEvent;
import io.citadel.api.event.bot.BotStoppedEvent;
import io.citadel.api.event.bot.BotStoppingEvent;
import io.citadel.api.event.world.WorldLoadedEvent;
import io.citadel.api.network.ProtocolState;
import io.citadel.api.proxy.ProxyDefinition;
import io.citadel.api.proxy.ProxyManager;
import io.citadel.api.reconnect.ReconnectPolicy;
import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import io.citadel.api.world.WorldManager;
import io.citadel.core.auth.AuthenticationService;
import io.citadel.core.net.Connection;
import io.citadel.core.net.Packet;
import io.citadel.core.net.PacketFraming;
import io.citadel.core.net.PacketRegistry;
import io.citadel.core.net.protocol.ConfigurationProtocolCodecs;
import io.citadel.core.net.protocol.FinishConfigurationPacket;
import io.citadel.core.net.protocol.KeepAlivePacket;
import io.citadel.core.net.protocol.play.PlayProtocolCodecs;
import io.citadel.core.world.WorldManagerImpl;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({
  "PMD.ExceptionAsFlowControl",
  "PMD.ExcessiveParameterList",
  "PMD.CyclomaticComplexity",
  "PMD.ExcessiveImports",
  "PMD.CouplingBetweenObjects"
})
public final class BotImpl implements Bot {

  private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
  private static final int DEFAULT_READ_TIMEOUT = 30000;

  private static final int DEFAULT_RECONNECT_MAX_ATTEMPTS = 10;
  private static final long DEFAULT_RECONNECT_INITIAL_DELAY_MS = 2_000;
  private static final long DEFAULT_RECONNECT_MAX_DELAY_MS = 60_000;

  private static final long HEALTH_CHECK_INTERVAL_MS = 1_000;
  private static final long SLEEP_CHUNK_MS = 500;

  private final UUID botId;
  private final String accountId;
  private final AccountManager accountManager;
  private final ProxyManager proxyManager;
  private final AuthenticationService authenticationService;
  private final EventBus eventBus;
  private final Logger logger;
  private final int connectTimeout;
  private final int readTimeout;
  private final String serverHost;
  private final int serverPort;
  private final ReconnectPolicy reconnectPolicy;

  private final AtomicReference<BotState> state;
  private final AtomicInteger reconnectAttempt;
  private final Object lock;

  private volatile Account account;
  private volatile Connection connection;
  private volatile Session session;
  private final WorldManagerImpl worldManager;
  private CompletableFuture<Void> startFuture;
  private CompletableFuture<Void> stopFuture;

  volatile CountDownLatch startBlocker;
  volatile CountDownLatch stopBlocker;
  volatile CountDownLatch reconnectBlocker;

  public BotImpl(
      String accountId,
      AccountManager accountManager,
      ProxyManager proxyManager,
      AuthenticationService authenticationService,
      EventBus eventBus,
      Logger logger,
      Configuration config) {
    this.botId = UUID.randomUUID();
    this.accountId = Objects.requireNonNull(accountId, "accountId");
    this.accountManager = Objects.requireNonNull(accountManager, "accountManager");
    this.proxyManager = proxyManager;
    this.authenticationService = authenticationService;
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    Objects.requireNonNull(config, "config");
    this.connectTimeout = config.getInt("networking.connect_timeout", DEFAULT_CONNECT_TIMEOUT);
    this.readTimeout = config.getInt("networking.read_timeout", DEFAULT_READ_TIMEOUT);
    this.serverHost = config.getString("networking.server.host", "localhost");
    this.serverPort = config.getInt("networking.server.port", 25565);
    this.reconnectPolicy = loadReconnectPolicy(config);
    this.worldManager = new WorldManagerImpl(botId, accountId, eventBus, logger);
    this.state = new AtomicReference<>(BotState.CREATED);
    this.reconnectAttempt = new AtomicInteger(0);
    this.lock = new Object();
  }

  BotImpl(
      String accountId,
      AccountManager accountManager,
      ProxyManager proxyManager,
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
    this.proxyManager = proxyManager;
    this.authenticationService = authenticationService;
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.serverHost = Objects.requireNonNull(serverHost, "serverHost");
    this.serverPort = serverPort;
    this.reconnectPolicy = ReconnectPolicy.defaults();
    this.worldManager = new WorldManagerImpl(botId, accountId, eventBus, logger);
    this.state = new AtomicReference<>(BotState.CREATED);
    this.reconnectAttempt = new AtomicInteger(0);
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
  public WorldManager getWorld() {
    return worldManager;
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
    synchronized (lock) {
      if (state.get() == BotState.FAILED) {
        state.set(BotState.STOPPED);
      }
    }
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
      ProxyDefinition proxyDef = resolveProxy(account);
      connection =
          new Connection(
              serverHost, serverPort, connectTimeout, readTimeout, eventBus, logger, proxyDef);
      connection.connect();
      if (!transitionTo(BotState.AUTHENTICATING)) {
        return;
      }
      session = authenticationService.login(connection, account);
      if (!transitionTo(BotState.RUNNING)) {
        return;
      }
      eventBus.publish(new WorldLoadedEvent(botId, accountId));
      connection.addPacketHandler(worldManager::handlePacket);
      logger.info(
          "Bot {} started (account={}, server={}:{})", botId, accountId, serverHost, serverPort);
      eventBus.publishAsync(new BotStartedEvent(botId, accountId, serverHost, serverPort));
      future.complete(null);
      startHealthCheck();
      startReceiveLoop();
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
      worldManager.clear();
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

  private void startHealthCheck() {
    if (!reconnectPolicy.enabled()) {
      return;
    }
    Thread.startVirtualThread(
        () -> {
          try {
            while (state.get() == BotState.RUNNING) {
              Thread.sleep(HEALTH_CHECK_INTERVAL_MS);
              if (state.get() != BotState.RUNNING) {
                return;
              }
              if (connection == null || !connection.isConnected()) {
                logger.warn("Bot {} connection lost, initiating reconnect", botId);
                initiateReconnect();
                return;
              }
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        });
  }

  private void startReceiveLoop() {
    PacketRegistry configRegistry =
        PacketRegistry.builder()
            .register(0x02, ConfigurationProtocolCodecs.finishConfiguration())
            .register(0x03, ConfigurationProtocolCodecs.keepAlive())
            .build();
    PacketRegistry playRegistry =
        PacketRegistry.builder()
            .register(0x0C, PlayProtocolCodecs.blockUpdate())
            .register(0x1E, PlayProtocolCodecs.chunkUnload())
            .register(0x25, PlayProtocolCodecs.chunkData())
            .register(0x3A, PlayProtocolCodecs.multiBlockUpdate())
            .build();
    FinishConfigurationPacket finishConfig = new FinishConfigurationPacket();
    Thread.startVirtualThread(() -> runReceiveLoop(configRegistry, playRegistry, finishConfig));
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private void runReceiveLoop(
      PacketRegistry configRegistry,
      PacketRegistry playRegistry,
      FinishConfigurationPacket finishConfig) {
    PacketRegistry registry = configRegistry;
    try {
      while (state.get() == BotState.RUNNING) {
        registry = receiveNextPacket(registry, playRegistry, finishConfig);
      }
    } catch (IOException e) {
      if (state.get() == BotState.RUNNING) {
        logger.warn("Bot {} receive loop lost connection, initiating reconnect", botId);
        initiateReconnect();
      }
    } catch (Exception e) {
      logger.error("Bot {} receive loop error: {}", botId, e.getMessage());
    }
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private PacketRegistry receiveNextPacket(
      PacketRegistry registry, PacketRegistry playRegistry, FinishConfigurationPacket finishConfig)
      throws IOException {
    try {
      Packet packet = connection.receivePacket(registry);
      if (packet instanceof KeepAlivePacket ka) {
        connection.sendPacket(new KeepAlivePacket(ka.getId()));
      } else if (packet instanceof FinishConfigurationPacket) {
        connection.sendPacket(finishConfig);
        connection.setProtocolState(ProtocolState.PLAY);
        return playRegistry;
      }
      return registry;
    } catch (PacketFraming.UnknownPacketException e) {
      logger.debug("Unknown packet in {} state: {}", connection.getProtocolState(), e.getMessage());
      return registry;
    }
  }

  void initiateReconnect() {
    synchronized (lock) {
      BotState current = state.get();
      if (current != BotState.RUNNING) {
        return;
      }
      if (!reconnectPolicy.enabled()) {
        state.set(BotState.FAILED);
        logger.error("Bot {} connection lost and reconnect is disabled", botId);
        eventBus.publishAsync(
            new BotReconnectFailedEvent(botId, accountId, 0, "Reconnect disabled"));
        return;
      }
      int attempt = reconnectAttempt.incrementAndGet();
      if (attempt > reconnectPolicy.maxAttempts()) {
        state.set(BotState.FAILED);
        logger.error(
            "Bot {} max reconnect attempts ({}) reached", botId, reconnectPolicy.maxAttempts());
        eventBus.publishAsync(
            new BotReconnectFailedEvent(
                botId, accountId, attempt, "Max reconnect attempts reached"));
        return;
      }
      logger.info(
          "Bot {} reconnecting (attempt {}/{})", botId, attempt, reconnectPolicy.maxAttempts());
      state.set(BotState.RECONNECTING);
      eventBus.publishAsync(
          new BotReconnectStartedEvent(botId, accountId, attempt, reconnectPolicy.maxAttempts()));
      startReconnectAttempt(attempt);
    }
  }

  private void startReconnectAttempt(int attempt) {
    long delay = reconnectPolicy.delayForAttempt(attempt - 1).toMillis();
    Thread.startVirtualThread(
        () -> {
          try {
            waitForDelayOrCancelled(delay);
            doReconnect(attempt);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        });
  }

  private void waitForDelayOrCancelled(long totalDelay) throws InterruptedException {
    long remaining = totalDelay;
    while (remaining > 0) {
      if (state.get() != BotState.RECONNECTING) {
        throw new InterruptedException("Reconnect cancelled");
      }
      long chunk = Math.min(remaining, SLEEP_CHUNK_MS);
      Thread.sleep(chunk);
      remaining -= chunk;
    }
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private void doReconnect(int attempt) {
    Connection newConn = null;
    Session newSession;
    try {
      if (reconnectBlocker != null) {
        reconnectBlocker.await();
      }
      if (!prepareReconnectState()) {
        return;
      }
      newConn = createReconnectConnection();
      if (!authenticateReconnectState()) {
        newConn.close();
        return;
      }
      newSession = authenticationService.login(newConn, account);
      if (!finalizeReconnectState(newConn, newSession)) {
        newConn.close();
        return;
      }
      newConn = null;
      eventBus.publish(new WorldLoadedEvent(botId, accountId));
      connection.addPacketHandler(worldManager::handlePacket);
      logger.info("Bot {} reconnected successfully (attempt {})", botId, attempt);
      eventBus.publishAsync(new BotReconnectSucceededEvent(botId, accountId, attempt));
      reconnectAttempt.set(0);
      startHealthCheck();
      startReceiveLoop();
    } catch (Exception e) {
      closeQuietly(newConn);
      handleReconnectFailure(attempt, e);
    }
  }

  private boolean prepareReconnectState() {
    synchronized (lock) {
      if (state.get() != BotState.RECONNECTING) {
        return false;
      }
      state.set(BotState.CONNECTING);
      return true;
    }
  }

  private Connection createReconnectConnection() throws IOException {
    ProxyDefinition proxyDef = resolveProxy(account);
    Connection conn =
        new Connection(
            serverHost, serverPort, connectTimeout, readTimeout, eventBus, logger, proxyDef);
    conn.connect();
    return conn;
  }

  private boolean authenticateReconnectState() {
    synchronized (lock) {
      BotState current = state.get();
      if (current != BotState.CONNECTING && current != BotState.RECONNECTING) {
        return false;
      }
      state.set(BotState.AUTHENTICATING);
      return true;
    }
  }

  private boolean finalizeReconnectState(Connection newConn, Session newSession) {
    synchronized (lock) {
      if (state.get() != BotState.AUTHENTICATING) {
        return false;
      }
      cleanup();
      this.connection = newConn;
      this.session = newSession;
      state.set(BotState.RUNNING);
      return true;
    }
  }

  private static void closeQuietly(Connection conn) {
    if (conn != null) {
      try {
        conn.close();
      } catch (Exception ignored) {
      }
    }
  }

  private void handleReconnectFailure(int attempt, Exception e) {
    synchronized (lock) {
      BotState current = state.get();
      if (current == BotState.STOPPING || current == BotState.STOPPED) {
        return;
      }
      if (attempt >= reconnectPolicy.maxAttempts()) {
        logger.error(
            "Bot {} reconnect failed after {} attempts: {}", botId, attempt, e.getMessage());
        state.set(BotState.FAILED);
        eventBus.publishAsync(
            new BotReconnectFailedEvent(botId, accountId, attempt, e.getMessage()));
      } else {
        logger.warn(
            "Bot {} reconnect attempt {} failed: {}, retrying...", botId, attempt, e.getMessage());
        eventBus.publishAsync(
            new BotReconnectFailedEvent(botId, accountId, attempt, e.getMessage()));
        state.set(BotState.RECONNECTING);
        startReconnectAttempt(attempt + 1);
      }
    }
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private boolean transitionTo(BotState target) {
    while (true) {
      BotState current = state.get();
      if (current == BotState.STOPPING || current == BotState.STOPPED) {
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

  void forceState(BotState target) {
    synchronized (lock) {
      state.set(target);
    }
  }

  int reconnectAttempts() {
    return reconnectAttempt.get();
  }

  BotImpl(
      String accountId,
      AccountManager accountManager,
      ProxyManager proxyManager,
      AuthenticationService authenticationService,
      EventBus eventBus,
      Logger logger,
      int connectTimeout,
      int readTimeout,
      String serverHost,
      int serverPort,
      ReconnectPolicy reconnectPolicy) {
    this.botId = UUID.randomUUID();
    this.accountId = Objects.requireNonNull(accountId, "accountId");
    this.accountManager = Objects.requireNonNull(accountManager, "accountManager");
    this.proxyManager = proxyManager;
    this.authenticationService = authenticationService;
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.serverHost = Objects.requireNonNull(serverHost, "serverHost");
    this.serverPort = serverPort;
    this.reconnectPolicy = Objects.requireNonNull(reconnectPolicy, "reconnectPolicy");
    this.worldManager = new WorldManagerImpl(botId, accountId, eventBus, logger);
    this.state = new AtomicReference<>(BotState.CREATED);
    this.reconnectAttempt = new AtomicInteger(0);
    this.lock = new Object();
  }

  private ProxyDefinition resolveProxy(Account acct) {
    if (acct.proxy().isEmpty()) {
      return null;
    }
    String proxyId = acct.proxy().get();
    if (proxyManager == null) {
      throw new IllegalStateException(
          "Account " + acct.id() + " has proxy " + proxyId + " but no ProxyManager is available");
    }
    ProxyDefinition def = proxyManager.get(proxyId);
    if (def == null) {
      throw new IllegalStateException(
          "Account " + acct.id() + " has proxy " + proxyId + " but proxy not found");
    }
    return def;
  }

  private static ReconnectPolicy loadReconnectPolicy(Configuration config) {
    ConfigurationSection rc = config.getSection("reconnect");
    boolean enabled = rc.getBoolean("enabled", true);
    if (!enabled) {
      return ReconnectPolicy.disabled();
    }
    int maxAttempts = rc.getInt("maxAttempts", DEFAULT_RECONNECT_MAX_ATTEMPTS);
    long initialDelay = rc.getLong("initialDelay", DEFAULT_RECONNECT_INITIAL_DELAY_MS);
    long maxDelay = rc.getLong("maxDelay", DEFAULT_RECONNECT_MAX_DELAY_MS);
    return new ReconnectPolicy(true, maxAttempts, initialDelay, maxDelay);
  }
}
