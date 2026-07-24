package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.event.Event;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.network.ConnectionClosedEvent;
import io.citadel.api.event.network.ConnectionOpenedEvent;
import io.citadel.api.event.network.PacketReceivedEvent;
import io.citadel.api.event.network.PacketSentEvent;
import io.citadel.api.network.ServerListPingStatus;
import io.citadel.api.service.Logger;
import io.citadel.core.event.EventBusImpl;
import java.io.PrintStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class RealServerStatusTest {

  private static String fmt(String format, Object... args) {
    if (args == null || args.length == 0) {
      return format;
    }
    StringBuilder sb = new StringBuilder();
    int argIdx = 0;
    int i = 0;
    while (i < format.length()) {
      if (i + 1 < format.length() && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
        if (argIdx < args.length) {
          sb.append(args[argIdx++]);
        } else {
          sb.append("{}");
        }
        i += 2;
      } else {
        sb.append(format.charAt(i));
        i++;
      }
    }
    return sb.toString();
  }

  @Test
  void queryHypixel() throws Exception {
    PrintStream out = System.out;
    StatusLogger logger = new StatusLogger("StatusTest", out);
    EventBusImpl eventBus = new EventBusImpl(logger);

    eventBus.subscribe(
        Event.class,
        new EventHandler<>() {
          @Override
          public void handle(Event event) {
            if (event instanceof ConnectionOpenedEvent) {
              ConnectionOpenedEvent e = (ConnectionOpenedEvent) event;
              logger.info(
                  "Event: connected to {}:{} (prev={})",
                  e.getRemoteAddress(),
                  e.getRemotePort(),
                  e.getPreviousState());
            } else if (event instanceof ConnectionClosedEvent) {
              ConnectionClosedEvent e = (ConnectionClosedEvent) event;
              logger.info(
                  "Event: disconnected from {}:{} reason={} (prev={})",
                  e.getRemoteAddress(),
                  e.getRemotePort(),
                  e.getReason(),
                  e.getPreviousState());
            } else if (event instanceof PacketSentEvent) {
              PacketSentEvent e = (PacketSentEvent) event;
              logger.debug(
                  "Event: sent packet id=0x{} state={} size={}",
                  Integer.toHexString(e.getPacketId()),
                  e.getProtocolState(),
                  e.getPayloadSize());
            } else if (event instanceof PacketReceivedEvent) {
              PacketReceivedEvent e = (PacketReceivedEvent) event;
              logger.debug(
                  "Event: received packet id=0x{} state={} size={}",
                  Integer.toHexString(e.getPacketId()),
                  e.getProtocolState(),
                  e.getPayloadSize());
            }
          }
        });

    NetworkClient client = new NetworkClient(eventBus, logger);
    try {
      logger.info("Querying mc.hypixel.net:25565 ...");
      ServerListPingStatus status = client.queryStatus("mc.hypixel.net", 25565);
      logger.info("=== Server Status ===");
      logger.info("Version: {} (protocol {})", status.getVersion(), status.getProtocolVersion());
      logger.info("Players: {}/{}", status.getOnlinePlayers(), status.getMaxPlayers());
      logger.info("MOTD: {}", status.getMotd());
      assertNotNull(status.getVersion(), "version should not be null");
      assertTrue(status.getMaxPlayers() > 0, "max players should be > 0");
      out.println();
      out.println("REAL SERVER TEST PASSED: successfully queried mc.hypixel.net");
    } finally {
      client.shutdown();
      eventBus.shutdown();
    }
  }

  @Test
  void queryLocalhost() throws Exception {
    StatusLogger logger = new StatusLogger("StatusTest", System.out);
    EventBusImpl eventBus = new EventBusImpl(logger);
    NetworkClient client = new NetworkClient(eventBus, logger);
    try {
      logger.info("Querying localhost:25565 ...");
      ServerListPingStatus status = client.queryStatus("localhost", 25565);
      logger.info("=== Server Status ===");
      logger.info("Version: {} (protocol {})", status.getVersion(), status.getProtocolVersion());
      logger.info("Players: {}/{}", status.getOnlinePlayers(), status.getMaxPlayers());
      logger.info("MOTD: {}", status.getMotd());
    } catch (Exception e) {
      logger.warn("Localhost query failed (expected if no local server): {}", e.getMessage());
    } finally {
      client.shutdown();
      eventBus.shutdown();
    }
  }

  private static final class StatusLogger implements Logger {
    private final String name;
    private final PrintStream out;

    StatusLogger(String name, PrintStream out) {
      this.name = name;
      this.out = out;
    }

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

    private String prefix() {
      return "[" + name + "]";
    }

    @Override
    public void trace(String message) {
      out.println(prefix() + " TRACE: " + message);
    }

    @Override
    public void trace(String format, Object... args) {
      out.println(prefix() + " TRACE: " + fmt(format, args));
    }

    @Override
    public void trace(String message, Throwable throwable) {
      out.println(prefix() + " TRACE: " + message);
    }

    @Override
    public void trace(String accountName, String message) {}

    @Override
    public void trace(String accountName, String format, Object... args) {}

    @Override
    public void trace(String accountName, String message, Throwable throwable) {}

    @Override
    public void debug(String message) {
      out.println(prefix() + " DEBUG: " + message);
    }

    @Override
    public void debug(String format, Object... args) {
      out.println(prefix() + " DEBUG: " + fmt(format, args));
    }

    @Override
    public void debug(String message, Throwable throwable) {
      out.println(prefix() + " DEBUG: " + message);
    }

    @Override
    public void debug(String accountName, String message) {}

    @Override
    public void debug(String accountName, String format, Object... args) {}

    @Override
    public void debug(String accountName, String message, Throwable throwable) {}

    @Override
    public void info(String message) {
      out.println(prefix() + " INFO: " + message);
    }

    @Override
    public void info(String format, Object... args) {
      out.println(prefix() + " INFO: " + fmt(format, args));
    }

    @Override
    public void info(String message, Throwable throwable) {
      out.println(prefix() + " INFO: " + message);
    }

    @Override
    public void info(String accountName, String message) {}

    @Override
    public void info(String accountName, String format, Object... args) {}

    @Override
    public void info(String accountName, String message, Throwable throwable) {}

    @Override
    public void warn(String message) {
      out.println(prefix() + " WARN: " + message);
    }

    @Override
    public void warn(String format, Object... args) {
      out.println(prefix() + " WARN: " + fmt(format, args));
    }

    @Override
    public void warn(String message, Throwable throwable) {
      out.println(prefix() + " WARN: " + message);
    }

    @Override
    public void warn(String accountName, String message) {}

    @Override
    public void warn(String accountName, String format, Object... args) {}

    @Override
    public void warn(String accountName, String message, Throwable throwable) {}

    @Override
    public void error(String message) {
      out.println(prefix() + " ERROR: " + message);
    }

    @Override
    public void error(String format, Object... args) {
      out.println(prefix() + " ERROR: " + fmt(format, args));
    }

    @Override
    public void error(String message, Throwable throwable) {
      out.println(prefix() + " ERROR: " + message);
    }

    @Override
    public void error(String accountName, String message) {}

    @Override
    public void error(String accountName, String format, Object... args) {}

    @Override
    public void error(String accountName, String message, Throwable throwable) {}
  }
}
