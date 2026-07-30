package io.citadel.core.net;

import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.protocol.EncryptionRequestPacket;
import io.citadel.core.net.protocol.HandshakePacket;
import io.citadel.core.net.protocol.LoginProtocolCodecs;
import io.citadel.core.net.protocol.LoginStartPacket;
import io.citadel.core.net.protocol.LoginSuccessPacket;

public final class ConnectionLoginTest {
  public static void main(String[] args) throws Exception {
    String host = args.length > 0 ? args[0] : "play.6b6t.org";
    int port = args.length > 1 ? Integer.parseInt(args[1]) : 25565;
    
    EventBus silentBus = new EventBus() {
      public <T extends io.citadel.api.event.Event> Subscription subscribe(Class<T> t, EventHandler<T> h) { return () -> {}; }
      public <T extends io.citadel.api.event.Event> void unsubscribe(Class<T> t, EventHandler<T> h) {}
      public void publish(io.citadel.api.event.Event e) {}
      public void publishAsync(io.citadel.api.event.Event e) {}
    };
    
    Connection conn = new Connection(host, port, 15000, 20000, silentBus, new io.citadel.api.service.Logger() {
      public boolean isTraceEnabled() { return false; }
      public boolean isDebugEnabled() { return false; }
      public boolean isInfoEnabled() { return false; }
      public boolean isWarnEnabled() { return false; }
      public boolean isErrorEnabled() { return false; }
      public void trace(String m) {} public void trace(String f, Object... a) {} public void trace(String m, Throwable t) {} public void trace(String a, String m) {} public void trace(String a, String f, Object... v) {} public void trace(String a, String m, Throwable t) {}
      public void debug(String m) {} public void debug(String f, Object... a) {} public void debug(String m, Throwable t) {} public void debug(String a, String m) {} public void debug(String a, String f, Object... v) {} public void debug(String a, String m, Throwable t) {}
      public void info(String m) {} public void info(String f, Object... a) {} public void info(String m, Throwable t) {} public void info(String a, String m) {} public void info(String a, String f, Object... v) {} public void info(String a, String m, Throwable t) {}
      public void warn(String m) {} public void warn(String f, Object... a) {} public void warn(String m, Throwable t) {} public void warn(String a, String m) {} public void warn(String a, String f, Object... v) {} public void warn(String a, String m, Throwable t) {}
      public void error(String m) {} public void error(String f, Object... a) {} public void error(String m, Throwable t) {} public void error(String a, String m) {} public void error(String a, String f, Object... v) {} public void error(String a, String m, Throwable t) {}
    });
    
    System.out.println("Connecting...");
    conn.connect();
    System.out.println("Connected");
    
    // Build registry for LOGIN state
    PacketRegistry registry = PacketRegistry.builder()
      .register(0x00, LoginProtocolCodecs.disconnect())
      .register(0x01, LoginProtocolCodecs.encryptionRequest())
      .register(0x02, LoginProtocolCodecs.loginSuccess())
      .build();
    
    System.out.println("Sending Handshake...");
    conn.sendPacket(new HandshakePacket(NetworkClient.MINECRAFT_PROTOCOL_VERSION, host, port, 2));
    System.out.println("Setting protocol state to LOGIN...");
    conn.setProtocolState(ProtocolState.LOGIN);
    System.out.println("Sending LoginStart...");
    java.util.UUID uuid = java.util.UUID.randomUUID();
    conn.sendPacket(new LoginStartPacket("TestBot", uuid));
    System.out.println("Receiving response...");
    
    try {
      var packet = conn.receivePacket(registry);
      System.out.println("Received: " + packet.getClass().getSimpleName());
      if (packet instanceof EncryptionRequestPacket erp) {
        System.out.println("EncryptionRequest: serverId=" + erp.getServerId());
        System.out.println("PublicKey length: " + erp.getPublicKey().length);
        System.out.println("VerifyToken length: " + erp.getVerifyToken().length);
      } else if (packet instanceof LoginSuccessPacket) {
        System.out.println("LoginSuccess!");
      }
    } catch (Exception e) {
      System.out.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      e.printStackTrace(System.out);
    }
    
    conn.close();
    System.out.println("Done");
  }
}
