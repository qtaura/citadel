package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.protocol.HandshakePacket;
import io.citadel.core.net.protocol.PingRequestPacket;
import io.citadel.core.net.protocol.PingResponsePacket;
import io.citadel.core.net.protocol.StatusRequestPacket;
import io.citadel.core.net.protocol.StatusResponsePacket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.junit.jupiter.api.Test;

class StatusPacketTest {

  @Test
  void handshakeRoundTrip() throws Exception {
    HandshakePacket original = new HandshakePacket(767, "localhost", 25565, 1);
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(buf);
    original.write(out);
    out.flush();

    HandshakePacket decoded = new HandshakePacket();
    decoded.read(new DataInputStream(new ByteArrayInputStream(buf.toByteArray())));

    assertEquals(767, decoded.getProtocolVersion());
    assertEquals("localhost", decoded.getServerAddress());
    assertEquals(25565, decoded.getServerPort());
    assertEquals(1, decoded.getNextState());
  }

  @Test
  void handshakePacketId() {
    HandshakePacket p = new HandshakePacket();
    assertEquals(0x00, p.getPacketId(ProtocolState.HANDSHAKE));
  }

  @Test
  void statusRequestRoundTrip() throws Exception {
    StatusRequestPacket original = new StatusRequestPacket();
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(buf);
    original.write(out);
    out.flush();

    assertEquals(0, buf.size());

    StatusRequestPacket decoded = new StatusRequestPacket();
    decoded.read(new DataInputStream(new ByteArrayInputStream(new byte[0])));
  }

  @Test
  void statusRequestPacketId() {
    StatusRequestPacket p = new StatusRequestPacket();
    assertEquals(0x00, p.getPacketId(ProtocolState.STATUS));
  }

  @Test
  void statusResponseRoundTrip() throws Exception {
    String json = "{\"description\":\"A Minecraft Server\",\"players\":{\"max\":20,\"online\":3}}";
    StatusResponsePacket original = new StatusResponsePacket(json);
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(buf);
    original.write(out);
    out.flush();

    StatusResponsePacket decoded = new StatusResponsePacket();
    decoded.read(new DataInputStream(new ByteArrayInputStream(buf.toByteArray())));

    assertEquals(json, decoded.getJson());
  }

  @Test
  void statusResponsePacketId() {
    StatusResponsePacket p = new StatusResponsePacket();
    assertEquals(0x00, p.getPacketId(ProtocolState.STATUS));
  }

  @Test
  void pingRequestRoundTrip() throws Exception {
    long payload = 123456789L;
    PingRequestPacket original = new PingRequestPacket(payload);
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(buf);
    original.write(out);
    out.flush();

    PingRequestPacket decoded = new PingRequestPacket();
    decoded.read(new DataInputStream(new ByteArrayInputStream(buf.toByteArray())));

    assertEquals(payload, decoded.getPayload());
  }

  @Test
  void pingRequestPacketId() {
    PingRequestPacket p = new PingRequestPacket();
    assertEquals(0x01, p.getPacketId(ProtocolState.STATUS));
  }

  @Test
  void pingResponseRoundTrip() throws Exception {
    long payload = 987654321L;
    PingResponsePacket original = new PingResponsePacket(payload);
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(buf);
    original.write(out);
    out.flush();

    PingResponsePacket decoded = new PingResponsePacket();
    decoded.read(new DataInputStream(new ByteArrayInputStream(buf.toByteArray())));

    assertEquals(payload, decoded.getPayload());
  }

  @Test
  void pingResponsePacketId() {
    PingResponsePacket p = new PingResponsePacket();
    assertEquals(0x01, p.getPacketId(ProtocolState.STATUS));
  }
}
