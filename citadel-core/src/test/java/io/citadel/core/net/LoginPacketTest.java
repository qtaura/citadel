package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.protocol.DisconnectPacket;
import io.citadel.core.net.protocol.LoginProtocolCodecs;
import io.citadel.core.net.protocol.LoginStartPacket;
import io.citadel.core.net.protocol.LoginSuccessPacket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginPacketTest {

  @Test
  void loginStartRoundTrip() throws Exception {
    UUID uuid = UUID.randomUUID();
    LoginStartPacket original = new LoginStartPacket("Steve", uuid);

    LoginStartPacket decoded = roundTrip(original, new LoginStartPacket());

    assertEquals("Steve", decoded.getUsername());
    assertEquals(uuid, decoded.getProfileId());
  }

  @Test
  void loginStartUsesMinecraftStringEncoding() throws Exception {
    LoginStartPacket packet = new LoginStartPacket("Steve", new UUID(0L, 1L));
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    packet.write(new DataOutputStream(buf));

    byte[] bytes = buf.toByteArray();
    assertEquals(5, bytes[0]);
    assertEquals('S', bytes[1]);
  }

  @Test
  void loginStartPacketId() {
    assertEquals(0x00, new LoginStartPacket().getPacketId(ProtocolState.LOGIN));
  }

  @Test
  void loginSuccessRoundTrip() throws Exception {
    UUID uuid = UUID.randomUUID();
    LoginSuccessPacket original = new LoginSuccessPacket(uuid, "Alex", true);

    LoginSuccessPacket decoded = roundTrip(original, new LoginSuccessPacket());

    assertEquals(uuid, decoded.getProfileId());
    assertEquals("Alex", decoded.getUsername());
    assertTrue(decoded.isStrictErrorHandling());
  }

  @Test
  void loginSuccessPacketId() {
    assertEquals(0x02, new LoginSuccessPacket().getPacketId(ProtocolState.LOGIN));
  }

  @Test
  void disconnectRoundTrip() throws Exception {
    String reason = "{\"text\":\"Server closed\"}";
    DisconnectPacket original = new DisconnectPacket(reason);

    DisconnectPacket decoded = roundTrip(original, new DisconnectPacket());

    assertEquals(reason, decoded.getReasonJson());
  }

  @Test
  void loginCodecRegistryDecodesPackets() throws Exception {
    UUID uuid = UUID.randomUUID();
    PacketRegistry registry =
        PacketRegistry.builder()
            .register(0x00, LoginProtocolCodecs.disconnect())
            .register(0x02, LoginProtocolCodecs.loginSuccess())
            .build();
    byte[] frame = PacketFraming.encode(0x02, new LoginSuccessPacket(uuid, "Steve"));

    PacketFraming.FramedPacket framed =
        PacketFraming.readFrame(new DataInputStream(new ByteArrayInputStream(frame)));
    Packet decoded =
        PacketFraming.decode(
            framed.getPacketId(), framed.getPayload(), registry.getCodec(framed.getPacketId()));

    assertInstanceOf(LoginSuccessPacket.class, decoded);
    assertEquals(uuid, ((LoginSuccessPacket) decoded).getProfileId());
  }

  private static <T extends Packet> T roundTrip(T original, T decoded) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    original.write(new DataOutputStream(buf));
    decoded.read(new DataInputStream(new ByteArrayInputStream(buf.toByteArray())));
    return decoded;
  }
}
