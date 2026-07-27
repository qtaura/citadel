package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.core.net.protocol.StatusProtocolCodecs;
import io.citadel.core.net.protocol.StatusRequestPacket;
import io.citadel.core.net.protocol.StatusResponsePacket;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PacketFramingTest {

  private PacketRegistry registry;

  @BeforeEach
  void setUp() {
    registry =
        PacketRegistry.builder().register(0x00, StatusProtocolCodecs.statusResponse()).build();
  }

  @Test
  void encodeAndDecodeStatusResponse() throws Exception {
    StatusResponsePacket original = new StatusResponsePacket("{\"description\":\"test\"}");
    byte[] frame = PacketFraming.encode(0x00, original);

    PacketFraming.FramedPacket framed =
        PacketFraming.readFrame(new DataInputStream(new ByteArrayInputStream(frame)));
    assertEquals(0x00, framed.getPacketId());
    assertTrue(framed.getPayload().length > 0);

    PacketCodec codec = registry.getCodec(framed.getPacketId());
    assertNotNull(codec);
    Packet decoded = PacketFraming.decode(framed.getPacketId(), framed.getPayload(), codec);
    assertInstanceOf(StatusResponsePacket.class, decoded);
    assertEquals("{\"description\":\"test\"}", ((StatusResponsePacket) decoded).getJson());
  }

  @Test
  void encodeAndDecodeEmptyPacket() throws Exception {
    StatusRequestPacket original = new StatusRequestPacket();
    byte[] frame = PacketFraming.encode(0x00, original);

    PacketFraming.FramedPacket framed =
        PacketFraming.readFrame(new DataInputStream(new ByteArrayInputStream(frame)));
    assertEquals(0x00, framed.getPacketId());
    assertEquals(0, framed.getPayload().length);
  }

  @Test
  void decodeUnknownPacketReturnsNull() {
    PacketCodec codec = registry.getCodec(0x01);
    assertNull(codec);
  }

  @Test
  void readFrameWithNegativeLength() {
    byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F};
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
    assertThrows(Exception.class, () -> PacketFraming.readFrame(in));
  }

  @Test
  void readFrameWithTruncatedBody() {
    byte[] data = {0x05, 0x00};
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
    assertThrows(Exception.class, () -> PacketFraming.readFrame(in));
  }

  @Test
  void readFrameWithOnlyPacketIdNoPayload() throws Exception {
    byte[] data = {0x01, 0x00};
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
    PacketFraming.FramedPacket framed = PacketFraming.readFrame(in);
    assertEquals(0x00, framed.getPacketId());
    assertEquals(0, framed.getPayload().length);
  }

  @Test
  void negativeBodyLengthThrows() {
    byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F};
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
    IOException ex = assertThrows(IOException.class, () -> PacketFraming.readFrame(in));
    assertTrue(ex.getMessage().contains("Negative"));
  }
}
