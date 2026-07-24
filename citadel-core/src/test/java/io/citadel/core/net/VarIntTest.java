package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.junit.jupiter.api.Test;

class VarIntTest {

  @Test
  void writeAndReadZero() throws Exception {
    assertRoundTrip(0);
  }

  @Test
  void writeAndReadOne() throws Exception {
    assertRoundTrip(1);
  }

  @Test
  void writeAndRead127() throws Exception {
    assertRoundTrip(127);
  }

  @Test
  void writeAndRead128() throws Exception {
    assertRoundTrip(128);
  }

  @Test
  void writeAndRead255() throws Exception {
    assertRoundTrip(255);
  }

  @Test
  void writeAndReadMaxPositive() throws Exception {
    assertRoundTrip(Integer.MAX_VALUE);
  }

  @Test
  void writeAndReadNegativeOne() throws Exception {
    assertRoundTrip(-1);
  }

  @Test
  void writeAndReadNegativeMin() throws Exception {
    assertRoundTrip(Integer.MIN_VALUE);
  }

  @Test
  void sizeOfZero() {
    assertEquals(1, VarInt.size(0));
  }

  @Test
  void sizeOfOne() {
    assertEquals(1, VarInt.size(1));
  }

  @Test
  void sizeOf127() {
    assertEquals(1, VarInt.size(127));
  }

  @Test
  void sizeOf128() {
    assertEquals(2, VarInt.size(128));
  }

  @Test
  void sizeOfMax() {
    assertEquals(5, VarInt.size(Integer.MAX_VALUE));
  }

  @Test
  void sizeOfNegative() {
    assertEquals(5, VarInt.size(-1));
  }

  @Test
  void readFromStream() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    VarInt.writeToStream(300, buf);
    buf.flush();

    int result = VarInt.readFromStream(new ByteArrayInputStream(buf.toByteArray()));
    assertEquals(300, result);
  }

  @Test
  void readFromBytes() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    VarInt.writeToStream(42, buf);
    buf.flush();
    byte[] bytes = buf.toByteArray();
    assertEquals(42, VarInt.read(bytes, 0));
  }

  @Test
  void readVarIntTooLarge() {
    byte[] oversized = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00};
    assertThrows(IllegalArgumentException.class, () -> VarInt.read(oversized, 0));
  }

  @Test
  void readFromStreamEndOfStream() {
    ByteArrayInputStream empty = new ByteArrayInputStream(new byte[0]);
    assertThrows(java.io.IOException.class, () -> VarInt.readFromStream(empty));
  }

  @SuppressWarnings("PMD.SignatureDeclareThrowsException")
  private static void assertRoundTrip(int value) throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(buf);
    VarInt.write(value, out);
    out.flush();

    DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));
    int result = VarInt.read(in);
    assertEquals(value, result);
  }
}
