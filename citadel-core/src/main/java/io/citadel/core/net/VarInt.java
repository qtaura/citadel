package io.citadel.core.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class VarInt {

  private VarInt() {}

  public static void write(int value, DataOutput out) throws IOException {
    int v = value;
    do {
      byte temp = (byte) (v & 0x7F);
      v >>>= 7;
      if (v != 0) {
        temp |= 0x80;
      }
      out.writeByte(temp);
    } while (v != 0);
  }

  public static void writeToStream(int value, OutputStream out) throws IOException {
    int v = value;
    do {
      byte temp = (byte) (v & 0x7F);
      v >>>= 7;
      if (v != 0) {
        temp |= 0x80;
      }
      out.write(temp);
    } while (v != 0);
  }

  public static int read(DataInput in) throws IOException {
    int result = 0;
    int shift = 0;
    while (true) {
      byte b = in.readByte();
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
      if (shift >= 32) {
        throw new IOException("VarInt too large");
      }
    }
  }

  public static int readFromStream(InputStream in) throws IOException {
    int result = 0;
    int shift = 0;
    while (true) {
      int b = in.read();
      if (b == -1) {
        throw new IOException("End of stream reading VarInt");
      }
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
      if (shift >= 32) {
        throw new IOException("VarInt too large");
      }
    }
  }

  public static int read(byte[] bytes, int offset) {
    int result = 0;
    int shift = 0;
    int idx = offset;
    while (true) {
      byte b = bytes[idx++];
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
      if (shift >= 32) {
        throw new IllegalArgumentException("VarInt too large");
      }
    }
  }

  public static int size(int value) {
    for (int i = 1; i < 5; i++) {
      if ((value & (-1 << (i * 7))) == 0) {
        return i;
      }
    }
    return 5;
  }
}
