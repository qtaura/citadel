package io.citadel.core.net;

import io.citadel.api.event.EventBus;
import io.citadel.api.network.ProtocolState;
import io.citadel.api.network.ServerListPingStatus;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.Logger;
import io.citadel.core.net.protocol.HandshakePacket;
import io.citadel.core.net.protocol.PingRequestPacket;
import io.citadel.core.net.protocol.PingResponsePacket;
import io.citadel.core.net.protocol.StatusProtocolCodecs;
import io.citadel.core.net.protocol.StatusRequestPacket;
import io.citadel.core.net.protocol.StatusResponsePacket;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NetworkClient {

  private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
  private static final int DEFAULT_READ_TIMEOUT = 30000;
  public static final int MINECRAFT_PROTOCOL_VERSION = 771;

  private final EventBus eventBus;
  private final Logger logger;
  private final int connectTimeout;
  private final int readTimeout;
  private final ExecutorService executor;
  private final AtomicBoolean shutdownFlag;
  private final PacketRegistry statusRegistry;

  public NetworkClient(EventBus eventBus, Logger logger, Configuration config) {
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.connectTimeout = config.getInt("networking.connect_timeout", DEFAULT_CONNECT_TIMEOUT);
    this.readTimeout = config.getInt("networking.read_timeout", DEFAULT_READ_TIMEOUT);
    this.executor = Executors.newCachedThreadPool(r -> new Thread(r, "net-client"));
    this.shutdownFlag = new AtomicBoolean(false);
    this.statusRegistry =
        PacketRegistry.builder()
            .register(0x00, StatusProtocolCodecs.statusResponse())
            .register(0x01, StatusProtocolCodecs.pingResponse())
            .build();
  }

  public NetworkClient(EventBus eventBus, Logger logger) {
    this(eventBus, logger, new DefaultNetworkingConfig());
  }

  public Connection connect(String host, int port) {
    Connection connection =
        new Connection(host, port, connectTimeout, readTimeout, eventBus, logger);
    executor.submit(
        () -> {
          try {
            connection.connect();
          } catch (Exception e) {
            logger.error("Connection failed: {}", e.getMessage());
          }
        });
    return connection;
  }

  public ServerListPingStatus queryStatus(String host, int port) throws IOException {
    try (Connection connection = createConnection(host, port)) {
      connection.connect();
      performHandshake(connection, host, port);
      ServerListPingStatus status = performStatusRequest(connection);
      performPing(connection);
      return status;
    }
  }

  private void performHandshake(Connection connection, String host, int port) throws IOException {
    connection.sendPacket(new HandshakePacket(MINECRAFT_PROTOCOL_VERSION, host, port, 1));
    connection.setProtocolState(ProtocolState.STATUS);
  }

  private ServerListPingStatus performStatusRequest(Connection connection) throws IOException {
    connection.sendPacket(new StatusRequestPacket());
    Packet response = connection.receivePacket(statusRegistry);
    if (!(response instanceof StatusResponsePacket)) {
      throw new IOException("Expected StatusResponse, got: " + response.getClass().getSimpleName());
    }
    return parseStatusJson(((StatusResponsePacket) response).getJson());
  }

  private void performPing(Connection connection) throws IOException {
    long pingTime = System.currentTimeMillis();
    connection.sendPacket(new PingRequestPacket(pingTime));
    Packet pong = connection.receivePacket(statusRegistry);
    if (!(pong instanceof PingResponsePacket)) {
      throw new IOException("Expected PingResponse, got: " + pong.getClass().getSimpleName());
    }
    long pongTime = ((PingResponsePacket) pong).getPayload();
    if (pongTime != pingTime) {
      logger.warn("Ping payload mismatch: sent={} received={}", pingTime, pongTime);
    }
  }

  @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
  public void shutdown() {
    if (shutdownFlag.compareAndSet(false, true)) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  public boolean isShutdown() {
    return shutdownFlag.get();
  }

  private Connection createConnection(String host, int port) {
    return new Connection(host, port, connectTimeout, readTimeout, eventBus, logger);
  }

  static ServerListPingStatus parseStatusJson(String json) {
    return StatusJsonParser.parse(json);
  }

  PacketRegistry getStatusRegistry() {
    return statusRegistry;
  }

  private static final class DefaultNetworkingConfig implements Configuration {
    @Override
    public io.citadel.api.service.ConfigurationSection getRoot() {
      return this;
    }

    @Override
    public io.citadel.api.service.ConfigurationSection getPluginSection(String pluginName) {
      return this;
    }

    @Override
    public void reload() {}

    @Override
    public void addListener(io.citadel.api.service.ConfigurationListener listener) {}

    @Override
    public void removeListener(io.citadel.api.service.ConfigurationListener listener) {}

    @Override
    public String getString(String path) {
      return null;
    }

    @Override
    public String getString(String path, String defaultValue) {
      return defaultValue;
    }

    @Override
    public boolean getBoolean(String path) {
      return false;
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
      return defaultValue;
    }

    @Override
    public int getInt(String path) {
      return 0;
    }

    @Override
    public int getInt(String path, int defaultValue) {
      return defaultValue;
    }

    @Override
    public long getLong(String path) {
      return 0;
    }

    @Override
    public long getLong(String path, long defaultValue) {
      return defaultValue;
    }

    @Override
    public double getDouble(String path) {
      return 0;
    }

    @Override
    public double getDouble(String path, double defaultValue) {
      return defaultValue;
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
      return null;
    }

    @Override
    public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
      return defaultValue;
    }

    @Override
    public java.util.List<String> getStringList(String path) {
      return java.util.Collections.emptyList();
    }

    @Override
    public <T> java.util.List<T> getList(String path) {
      return java.util.Collections.emptyList();
    }

    @Override
    public io.citadel.api.service.ConfigurationSection getSection(String path) {
      return this;
    }

    @Override
    public boolean contains(String path) {
      return false;
    }

    @Override
    public java.util.Set<String> getKeys() {
      return java.util.Collections.emptySet();
    }
  }
}
