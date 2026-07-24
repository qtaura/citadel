package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.event.EventBus;
import io.citadel.api.network.ConnectionState;
import io.citadel.api.network.ProtocolState;
import io.citadel.api.service.Logger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConnectionTest {

  private static final int CONNECT_TIMEOUT = 3000;
  private static final int READ_TIMEOUT = 5000;

  @Test
  void initialStateIsCreated() {
    Connection c = createConnection("localhost", 25565);
    assertEquals(ConnectionState.CREATED, c.getState());
  }

  @Test
  void connectThrowsWhenAlreadyConnected() throws Exception {
    try (ServerSocket server = new ServerSocket(0)) {
      Connection c = createConnection("localhost", server.getLocalPort());
      c.connect();
      waitForConnected(c);
      assertThrows(IllegalStateException.class, c::connect);
      c.close();
    }
  }

  @Test
  void connectToUnreachablePortThrows() {
    Connection c = createConnection("localhost", 1);
    assertThrows(java.io.IOException.class, c::connect);
  }

  @Test
  void closeIsIdempotent() throws Exception {
    try (ServerSocket server = new ServerSocket(0)) {
      Connection c = createConnection("localhost", server.getLocalPort());
      c.connect();
      waitForConnected(c);
      c.close();
      c.close();
      assertEquals(ConnectionState.CLOSED, c.getState());
    }
  }

  @Test
  void closeFromCreatedDoesNotThrow() {
    Connection c = createConnection("localhost", 25565);
    c.close();
    assertEquals(ConnectionState.CLOSED, c.getState());
  }

  @Test
  void protocolStateDefaultsToHandshake() {
    Connection c = createConnection("localhost", 25565);
    assertEquals(ProtocolState.HANDSHAKE, c.getProtocolState());
  }

  @Test
  void protocolStateCanTransition() {
    Connection c = createConnection("localhost", 25565);
    c.setProtocolState(ProtocolState.STATUS);
    assertEquals(ProtocolState.STATUS, c.getProtocolState());
  }

  @Test
  void sendPacketBeforeConnectThrows() {
    Connection c = createConnection("localhost", 25565);
    assertThrows(
        IllegalStateException.class,
        () -> c.sendPacket(new io.citadel.core.net.protocol.StatusRequestPacket()));
  }

  @SuppressWarnings("PMD.UnusedLocalVariable")
  @Test
  void fullConnectionLifecycle() throws Exception {
    CountDownLatch serverGotConnection = new CountDownLatch(1);
    try (ServerSocket server = new ServerSocket(0)) {
      Thread serverThread =
          new Thread(
              () -> {
                try (Socket client = server.accept()) {
                  serverGotConnection.countDown();
                  Thread.sleep(500);
                } catch (Exception e) {
                  // ignore
                }
              });
      serverThread.start();

      Connection c = createConnection("localhost", server.getLocalPort());
      c.connect();
      assertTrue(serverGotConnection.await(5, TimeUnit.SECONDS));
      assertTrue(c.isConnected());

      c.close();
      assertFalse(c.isConnected());
      assertEquals(ConnectionState.CLOSED, c.getState());
      serverThread.join(2000);
    }
  }

  private static Connection createConnection(String host, int port) {
    return new Connection(
        host, port, CONNECT_TIMEOUT, READ_TIMEOUT, new NoOpEventBus(), new NoOpLogger());
  }

  private static void waitForConnected(Connection c) throws InterruptedException {
    for (int i = 0; i < 50; i++) {
      if (c.isConnected()) {
        return;
      }
      Thread.sleep(100);
    }
  }

  private static final class NoOpEventBus implements EventBus {
    @Override
    public <T extends io.citadel.api.event.Event> io.citadel.api.event.Subscription subscribe(
        Class<T> type, io.citadel.api.event.EventHandler<T> handler) {
      return () -> {};
    }

    @Override
    public <T extends io.citadel.api.event.Event> void unsubscribe(
        Class<T> type, io.citadel.api.event.EventHandler<T> handler) {}

    @Override
    public void publish(io.citadel.api.event.Event event) {}

    @Override
    public void publishAsync(io.citadel.api.event.Event event) {}
  }

  private static final class NoOpLogger implements Logger {
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
