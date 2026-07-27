package io.citadel.core.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class MinecraftStrings {

  private MinecraftStrings() {}

  public static void write(String value, DataOutput out) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    VarInt.write(bytes.length, out);
    out.write(bytes);
  }

  public static String read(DataInput in) throws IOException {
    int len = VarInt.read(in);
    if (len < 0) {
      throw new IOException("Negative string length: " + len);
    }
    byte[] bytes = new byte[len];
    in.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
