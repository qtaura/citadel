package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.event.EventBus;
import io.citadel.api.network.ServerListPingStatus;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import io.citadel.core.net.protocol.HandshakePacket;
import io.citadel.core.net.protocol.PingResponsePacket;
import io.citadel.core.net.protocol.StatusResponsePacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.SingularField"})
class NetworkClientTest {

  private NetworkClient client;
  private TestLogger logger;
  private TestEventBus eventBus;

  @BeforeEach
  void setUp() {
    logger = new TestLogger();
    eventBus = new TestEventBus();
    client = new NetworkClient(eventBus, logger, new TestConfig());
  }

  @AfterEach
  void tearDown() {
    client.shutdown();
  }

  @Test
  void queryStatusWithMockServer() throws Exception {
    String statusJson =
        "{\"version\":{\"name\":\"Mock\",\"protocol\":767},"
            + "\"players\":{\"max\":100,\"online\":5},"
            + "\"description\":{\"text\":\"Mock Server\"}}";
    long pingPayload = System.currentTimeMillis();

    try (MockServer server = new MockServer(statusJson, pingPayload)) {
      ServerListPingStatus status = client.queryStatus("localhost", server.getLocalPort());
      assertEquals(767, status.getProtocolVersion());
      assertEquals("Mock", status.getVersion());
      assertEquals("Mock Server", status.getMotd());
      assertEquals(100, status.getMaxPlayers());
      assertEquals(5, status.getOnlinePlayers());
    }
  }

  @Test
  void queryStatusEmitsEvents() throws Exception {
    String statusJson =
        "{\"version\":{\"name\":\"Test\",\"protocol\":1},"
            + "\"players\":{\"max\":10,\"online\":1},"
            + "\"description\":{\"text\":\"Test\"}}";
    long pingPayload = System.currentTimeMillis();

    eventBus.clear();

    try (MockServer server = new MockServer(statusJson, pingPayload)) {
      client.queryStatus("localhost", server.getLocalPort());
    }

    assertTrue(
        eventBus.events.size() >= 4,
        "Expected at least 4 events (opened, sent, received, closed), got: "
            + eventBus.events.size());
  }

  @Test
  void queryStatusToNonExistentServer() {
    assertThrows(Exception.class, () -> client.queryStatus("localhost", 1));
  }

  @Test
  void shutdownIsIdempotent() {
    client.shutdown();
    client.shutdown();
    assertTrue(client.isShutdown());
  }

  @Test
  void connectCreatesConnection() throws Exception {
    try (ServerSocket server = new ServerSocket(0)) {
      AtomicBoolean connected = new AtomicBoolean(false);
      Thread serverThread =
          new Thread(
              () -> {
                try (@SuppressWarnings("PMD.UnusedLocalVariable")
                    Socket s = server.accept()) {
                  connected.set(true);
                  Thread.sleep(500);
                } catch (Exception e) {
                }
              });
      serverThread.start();

      Connection conn = client.connect("localhost", server.getLocalPort());
      assertNotNull(conn);
      serverThread.join(2000);
      assertTrue(connected.get());
    }
  }

  @Test
  void parseStatusJsonDirectly() {
    String json =
        "{\"version\":{\"name\":\"Direct\",\"protocol\":100},"
            + "\"players\":{\"max\":50,\"online\":10},"
            + "\"description\":{\"text\":\"Direct Test\"}}";
    ServerListPingStatus status = NetworkClient.parseStatusJson(json);
    assertEquals(100, status.getProtocolVersion());
    assertEquals("Direct", status.getVersion());
    assertEquals("Direct Test", status.getMotd());
    assertEquals(50, status.getMaxPlayers());
    assertEquals(10, status.getOnlinePlayers());
  }

  private static final class MockServer implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final Thread thread;

    @SuppressWarnings({"PMD.UnusedLocalVariable", "PMD.CouplingBetweenObjects"})
    MockServer(String statusJson, long pingPayload) throws IOException {
      this.serverSocket = new ServerSocket(0);
      this.thread =
          new Thread(
              () -> {
                try (@SuppressWarnings("PMD.UnusedLocalVariable")
                    Socket s = serverSocket.accept()) {
                  DataInputStream in = new DataInputStream(s.getInputStream());
                  DataOutputStream out = new DataOutputStream(s.getOutputStream());

                  // Read handshake (validate it)
                  PacketFraming.FramedPacket handshakeFrame = PacketFraming.readFrame(in);
                  PacketCodec codec =
                      new PacketCodec() {
                        @Override
                        public int getPacketId() {
                          return 0x00;
                        }

                        @Override
                        public Packet create() {
                          return new HandshakePacket();
                        }
                      };
                  PacketFraming.decode(
                      handshakeFrame.getPacketId(), handshakeFrame.getPayload(), codec);

                  // Read status request
                  PacketFraming.readFrame(in);

                  // Send status response
                  out.write(PacketFraming.encode(0x00, new StatusResponsePacket(statusJson)));

                  // Read ping request
                  PacketFraming.FramedPacket pingFrame = PacketFraming.readFrame(in);
                  assertEquals(0x01, pingFrame.getPacketId());

                  // Send pong response
                  out.write(PacketFraming.encode(0x01, new PingResponsePacket(pingPayload)));
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
      this.thread.start();
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    int getLocalPort() {
      return serverSocket.getLocalPort();
    }

    @Override
    public void close() {
      try {
        serverSocket.close();
      } catch (Exception e) {
      }
      try {
        thread.join(2000);
      } catch (Exception e) {
      }
    }
  }

  private static final class TestEventBus implements EventBus {
    final List<io.citadel.api.event.Event> events = new ArrayList<>();

    void clear() {
      events.clear();
    }

    @Override
    public <T extends io.citadel.api.event.Event> io.citadel.api.event.Subscription subscribe(
        Class<T> type, io.citadel.api.event.EventHandler<T> handler) {
      return () -> {};
    }

    @Override
    public <T extends io.citadel.api.event.Event> void unsubscribe(
        Class<T> type, io.citadel.api.event.EventHandler<T> handler) {}

    @Override
    public void publish(io.citadel.api.event.Event event) {
      events.add(event);
    }

    @Override
    public void publishAsync(io.citadel.api.event.Event event) {
      events.add(event);
    }
  }

  private static final class TestLogger implements Logger {
    final List<String> messages = new ArrayList<>();

    @Override
    public boolean isTraceEnabled() {
      return true;
    }

    @Override
    public boolean isDebugEnabled() {
      return true;
    }

    @Override
    public boolean isInfoEnabled() {
      return true;
    }

    @Override
    public boolean isWarnEnabled() {
      return true;
    }

    @Override
    public boolean isErrorEnabled() {
      return true;
    }

    @Override
    public void trace(String message) {
      messages.add("[TRACE] " + message);
    }

    @Override
    public void trace(String format, Object... args) {
      messages.add("[TRACE] " + String.format(format, args));
    }

    @Override
    public void trace(String message, Throwable throwable) {
      messages.add("[TRACE] " + message);
    }

    @Override
    public void trace(String accountName, String message) {}

    @Override
    public void trace(String accountName, String format, Object... args) {}

    @Override
    public void trace(String accountName, String message, Throwable throwable) {}

    @Override
    public void debug(String message) {
      messages.add("[DEBUG] " + message);
    }

    @Override
    public void debug(String format, Object... args) {
      messages.add("[DEBUG] " + String.format(format, args));
    }

    @Override
    public void debug(String message, Throwable throwable) {}

    @Override
    public void debug(String accountName, String message) {}

    @Override
    public void debug(String accountName, String format, Object... args) {}

    @Override
    public void debug(String accountName, String message, Throwable throwable) {}

    @Override
    public void info(String message) {
      messages.add("[INFO] " + message);
    }

    @Override
    public void info(String format, Object... args) {
      messages.add("[INFO] " + String.format(format, args));
    }

    @Override
    public void info(String message, Throwable throwable) {}

    @Override
    public void info(String accountName, String message) {}

    @Override
    public void info(String accountName, String format, Object... args) {}

    @Override
    public void info(String accountName, String message, Throwable throwable) {}

    @Override
    public void warn(String message) {
      messages.add("[WARN] " + message);
    }

    @Override
    public void warn(String format, Object... args) {
      messages.add("[WARN] " + String.format(format, args));
    }

    @Override
    public void warn(String message, Throwable throwable) {}

    @Override
    public void warn(String accountName, String message) {}

    @Override
    public void warn(String accountName, String format, Object... args) {}

    @Override
    public void warn(String accountName, String message, Throwable throwable) {}

    @Override
    public void error(String message) {
      messages.add("[ERROR] " + message);
    }

    @Override
    public void error(String format, Object... args) {
      messages.add("[ERROR] " + String.format(format, args));
    }

    @Override
    public void error(String message, Throwable throwable) {}

    @Override
    public void error(String accountName, String message) {}

    @Override
    public void error(String accountName, String format, Object... args) {}

    @Override
    public void error(String accountName, String message, Throwable throwable) {}
  }

  private static final class TestConfig implements Configuration {
    @Override
    public ConfigurationSection getRoot() {
      return this;
    }

    @Override
    public ConfigurationSection getPluginSection(String pluginName) {
      return this;
    }

    @Override
    public void reload() {}

    @Override
    public void addListener(ConfigurationListener listener) {}

    @Override
    public void removeListener(ConfigurationListener listener) {}

    @Override
    public String getString(String path) {
      return "";
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
    public List<String> getStringList(String path) {
      return List.of();
    }

    @Override
    public <T> List<T> getList(String path) {
      return List.of();
    }

    @Override
    public ConfigurationSection getSection(String path) {
      return this;
    }

    @Override
    public boolean contains(String path) {
      return false;
    }

    @Override
    public Set<String> getKeys() {
      return Set.of();
    }
  }
}
