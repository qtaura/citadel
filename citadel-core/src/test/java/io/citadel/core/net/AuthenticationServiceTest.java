package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.account.Account;
import io.citadel.api.auth.Session;
import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.event.auth.LoginFailedEvent;
import io.citadel.api.event.auth.LoginStartedEvent;
import io.citadel.api.event.auth.LoginSucceededEvent;
import io.citadel.api.network.ProtocolState;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import io.citadel.core.auth.AuthenticationException;
import io.citadel.core.auth.AuthenticationService;
import io.citadel.core.net.protocol.DisconnectPacket;
import io.citadel.core.net.protocol.HandshakePacket;
import io.citadel.core.net.protocol.LoginStartPacket;
import io.citadel.core.net.protocol.LoginSuccessPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.ExcessiveImports"})
class AuthenticationServiceTest {

  private static final int CONNECT_TIMEOUT = 3000;
  private static final int READ_TIMEOUT = 5000;

  @Test
  void loginSuccessTransitionsToConfigurationAndPublishesEvents() throws Exception {
    RecordingEventBus eventBus = new RecordingEventBus();
    TestLogger logger = new TestLogger();
    UUID profileId = UUID.randomUUID();

    try (LoginMockServer server = LoginMockServer.success(profileId, "Steve")) {
      Connection connection = connect(eventBus, logger, server.getLocalPort(), READ_TIMEOUT);
      AuthenticationService auth = new AuthenticationService(eventBus, logger, new TestConfig());

      Session session = auth.login(connection, Account.offline("Steve"));

      assertEquals("Steve", session.username());
      assertEquals(profileId, session.profileId());
      assertEquals(ProtocolState.CONFIGURATION, connection.getProtocolState());
      assertTrue(eventBus.contains(LoginStartedEvent.class));
      assertTrue(eventBus.contains(LoginSucceededEvent.class));
      connection.close();
    }
  }

  @Test
  void disconnectPacketFailsLoginAndClosesConnection() throws Exception {
    RecordingEventBus eventBus = new RecordingEventBus();
    TestLogger logger = new TestLogger();

    try (LoginMockServer server = LoginMockServer.disconnect("{\"text\":\"Nope\"}")) {
      Connection connection = connect(eventBus, logger, server.getLocalPort(), READ_TIMEOUT);
      AuthenticationService auth = new AuthenticationService(eventBus, logger, new TestConfig());

      AuthenticationException ex =
          assertThrows(
              AuthenticationException.class,
              () -> auth.login(connection, Account.offline("Steve")));

      assertTrue(ex.getMessage().contains("Server disconnected during login"));
      assertFalse(connection.isConnected());
      assertTrue(eventBus.contains(LoginFailedEvent.class));
    }
  }

  @Test
  void loginTimeoutFailsAndClosesConnection() throws Exception {
    RecordingEventBus eventBus = new RecordingEventBus();
    TestLogger logger = new TestLogger();

    try (LoginMockServer server = LoginMockServer.timeout()) {
      Connection connection = connect(eventBus, logger, server.getLocalPort(), READ_TIMEOUT);
      AuthenticationService auth =
          new AuthenticationService(eventBus, logger, new TestConfig(true, 100));

      AuthenticationException ex =
          assertThrows(
              AuthenticationException.class,
              () -> auth.login(connection, Account.offline("Steve")));

      assertEquals("Login timed out", ex.getMessage());
      assertFalse(connection.isConnected());
      assertTrue(eventBus.contains(LoginFailedEvent.class));
    }
  }

  @Test
  void offlineModeCanBeDisabledByConfiguration() throws Exception {
    RecordingEventBus eventBus = new RecordingEventBus();
    TestLogger logger = new TestLogger();

    try (LoginMockServer server = LoginMockServer.timeout()) {
      Connection connection = connect(eventBus, logger, server.getLocalPort(), READ_TIMEOUT);
      AuthenticationService auth =
          new AuthenticationService(eventBus, logger, new TestConfig(false, 1000));

      AuthenticationException ex =
          assertThrows(
              AuthenticationException.class,
              () -> auth.login(connection, Account.offline("Steve")));

      assertEquals("Offline accounts are disabled by configuration", ex.getCause().getMessage());
      assertFalse(connection.isConnected());
      assertTrue(eventBus.contains(LoginFailedEvent.class));
    }
  }

  @Test
  void readsConfigurationDefaults() {
    AuthenticationService auth =
        new AuthenticationService(new RecordingEventBus(), new TestLogger(), new TestConfig());

    assertTrue(auth.isOfflineMode());
    assertEquals(10000, auth.getLoginTimeout());
  }

  private static Connection connect(EventBus eventBus, Logger logger, int port, int readTimeout)
      throws IOException {
    Connection connection =
        new Connection("localhost", port, CONNECT_TIMEOUT, readTimeout, eventBus, logger);
    connection.connect();
    return connection;
  }

  private static final class LoginMockServer implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final Thread thread;

    static LoginMockServer success(UUID profileId, String username) throws IOException {
      return new LoginMockServer(new LoginSuccessPacket(profileId, username));
    }

    static LoginMockServer disconnect(String reasonJson) throws IOException {
      return new LoginMockServer(new DisconnectPacket(reasonJson));
    }

    static LoginMockServer timeout() throws IOException {
      return new LoginMockServer(null);
    }

    LoginMockServer(Packet response) throws IOException {
      this.serverSocket = new ServerSocket(0);
      this.thread = new Thread(() -> run(response));
      this.thread.start();
    }

    int getLocalPort() {
      return serverSocket.getLocalPort();
    }

    private void run(Packet response) {
      try (Socket socket = serverSocket.accept()) {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        PacketFraming.FramedPacket handshake = PacketFraming.readFrame(in);
        PacketFraming.decode(handshake.getPacketId(), handshake.getPayload(), handshakeCodec());
        PacketFraming.FramedPacket loginStart = PacketFraming.readFrame(in);
        PacketFraming.decode(loginStart.getPacketId(), loginStart.getPayload(), loginStartCodec());
        if (response != null) {
          out.write(PacketFraming.encode(response.getPacketId(ProtocolState.LOGIN), response));
          out.flush();
        } else {
          Thread.sleep(500);
        }
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    private static PacketCodec handshakeCodec() {
      return new PacketCodec() {
        @Override
        public int getPacketId() {
          return 0x00;
        }

        @Override
        public Packet create() {
          return new HandshakePacket();
        }
      };
    }

    private static PacketCodec loginStartCodec() {
      return new PacketCodec() {
        @Override
        public int getPacketId() {
          return 0x00;
        }

        @Override
        public Packet create() {
          return new LoginStartPacket();
        }
      };
    }

    @Override
    public void close() {
      try {
        serverSocket.close();
      } catch (IOException e) {
        // ignore test cleanup failures
      }
      try {
        thread.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static final class RecordingEventBus implements EventBus {
    private final List<Event> events = new ArrayList<>();

    boolean contains(Class<? extends Event> type) {
      return events.stream().anyMatch(type::isInstance);
    }

    @Override
    public <T extends Event> Subscription subscribe(Class<T> type, EventHandler<T> handler) {
      return () -> {};
    }

    @Override
    public <T extends Event> void unsubscribe(Class<T> type, EventHandler<T> handler) {}

    @Override
    public void publish(Event event) {
      events.add(event);
    }

    @Override
    public void publishAsync(Event event) {
      events.add(event);
    }
  }

  private static final class TestConfig implements Configuration {
    private final boolean offlineMode;
    private final int loginTimeout;

    TestConfig() {
      this(true, 10000);
    }

    TestConfig(boolean offlineMode, int loginTimeout) {
      this.offlineMode = offlineMode;
      this.loginTimeout = loginTimeout;
    }

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
      if ("authentication.offline_mode".equals(path)) {
        return offlineMode;
      }
      return defaultValue;
    }

    @Override
    public int getInt(String path) {
      return 0;
    }

    @Override
    public int getInt(String path, int defaultValue) {
      if ("connection.login_timeout".equals(path)) {
        return loginTimeout;
      }
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

  private static final class TestLogger implements Logger {
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
}
