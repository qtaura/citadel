package io.citadel.core.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class PacketFraming {

  private PacketFraming() {}

  public static byte[] encode(int packetId, Packet packet) throws IOException {
    return encode(packetId, packet, -1);
  }

  public static byte[] encode(int packetId, Packet packet, int compressionThreshold) throws IOException {
    try {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(buf);
      VarInt.write(packetId, out);
      packet.write(out);
      out.flush();
      byte[] body = buf.toByteArray();

      ByteArrayOutputStream frame = new ByteArrayOutputStream();
      DataOutputStream frameOut = new DataOutputStream(frame);

      if (compressionThreshold >= 0) {
        if (body.length >= compressionThreshold) {
          byte[] compressed = compress(body);
          VarInt.write(compressed.length + VarInt.size(body.length), frameOut);
          VarInt.write(body.length, frameOut);
          frameOut.write(compressed);
        } else {
          VarInt.write(body.length, frameOut);
          VarInt.write(0, frameOut);
          frameOut.write(body);
        }
      } else {
        VarInt.write(body.length, frameOut);
        frameOut.write(body);
      }
      frameOut.flush();
      return frame.toByteArray();
    } catch (Exception e) {
      throw new IOException("Packet encoding failed", e);
    }
  }

  public static FramedPacket readFrame(DataInputStream in) throws IOException {
    return readFrame(in, -1);
  }

  public static FramedPacket readFrame(DataInputStream in, int compressionThreshold) throws IOException {
    int bodyLength = VarInt.read(in);
    if (bodyLength < 0) {
      throw new IOException("Negative frame length: " + bodyLength);
    }

    byte[] data;
    if (compressionThreshold >= 0) {
      int decompressedLength = VarInt.read(in);
      int remaining = bodyLength - VarInt.size(decompressedLength);
      byte[] compressed = new byte[remaining];
      in.readFully(compressed);
      if (decompressedLength > 0) {
        data = decompress(compressed, decompressedLength);
      } else {
        data = compressed;
      }
    } else {
      data = new byte[bodyLength];
      in.readFully(data);
    }

    DataInputStream bodyIn = new DataInputStream(new ByteArrayInputStream(data));
    int packetId = VarInt.read((java.io.DataInput) bodyIn);
    int headerSize = VarInt.size(packetId);
    if (headerSize > data.length) {
      throw new IOException("Invalid frame: header size " + headerSize + " exceeds body length " + data.length);
    }
    byte[] payload = new byte[data.length - headerSize];
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

  private static byte[] compress(byte[] data) throws IOException {
    Deflater deflater = new Deflater();
    deflater.setInput(data);
    deflater.finish();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
    byte[] buffer = new byte[1024];
    while (!deflater.finished()) {
      int count = deflater.deflate(buffer);
      outputStream.write(buffer, 0, count);
    }
    deflater.end();
    return outputStream.toByteArray();
  }

  private static byte[] decompress(byte[] data, int decompressedLength) throws IOException {
    try {
      Inflater inflater = new Inflater();
      inflater.setInput(data);
      byte[] output = new byte[decompressedLength];
      int resultLength = inflater.inflate(output);
      inflater.end();
      if (resultLength != decompressedLength) {
        throw new IOException("Decompression size mismatch: expected " + decompressedLength + ", got " + resultLength);
      }
      return output;
    } catch (java.util.zip.DataFormatException e) {
      throw new IOException("Failed to decompress packet data", e);
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
