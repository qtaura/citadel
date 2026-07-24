package io.citadel.core.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class PacketFraming {

  private PacketFraming() {}

  public static byte[] encode(int packetId, Packet packet) throws IOException {
    try {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(buf);
      VarInt.write(packetId, out);
      packet.write(out);
      out.flush();
      byte[] body = buf.toByteArray();

      ByteArrayOutputStream frame = new ByteArrayOutputStream();
      DataOutputStream frameOut = new DataOutputStream(frame);
      VarInt.write(body.length, frameOut);
      frameOut.write(body);
      frameOut.flush();
      return frame.toByteArray();
    } catch (Exception e) {
      throw new IOException("Packet encoding failed", e);
    }
  }

  public static FramedPacket readFrame(DataInputStream in) throws IOException {
    int bodyLength = VarInt.read(in);
    if (bodyLength < 0) {
      throw new IOException("Negative frame length: " + bodyLength);
    }
    byte[] body = new byte[bodyLength];
    in.readFully(body);
    DataInputStream bodyIn = new DataInputStream(new ByteArrayInputStream(body));
    int packetId = VarInt.read((java.io.DataInput) bodyIn);
    int headerSize = VarInt.size(packetId);
    byte[] payload = new byte[bodyLength - headerSize];
    if (payload.length > 0) {
      bodyIn.readFully(payload);
    }
    return new FramedPacket(packetId, payload);
  }

  public static Packet decode(int packetId, byte[] payload, PacketCodec codec) throws IOException {
    try {
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
      Packet packet = codec.create();
      packet.read(in);
      return packet;
    } catch (Exception e) {
      throw new IOException("Packet decoding failed", e);
    }
  }

  public static final class FramedPacket {
    private final int packetId;
    private final byte[] payload;

    FramedPacket(int packetId, byte[] payload) {
      this.packetId = packetId;
      this.payload = payload.clone();
    }

    public int getPacketId() {
      return packetId;
    }

    public byte[] getPayload() {
      return payload.clone();
    }
  }

  public static final class UnknownPacketException extends IOException {
    private static final long serialVersionUID = 1L;

    public UnknownPacketException(String message) {
      super(message);
    }
  }
}
