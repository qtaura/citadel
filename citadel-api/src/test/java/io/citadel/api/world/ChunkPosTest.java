package io.citadel.api.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChunkPosTest {

  @Test
  void constructorStoresCoordinates() {
    ChunkPos pos = new ChunkPos(1, 2);
    assertEquals(1, pos.x());
    assertEquals(2, pos.z());
  }

  @Test
  void negativeCoordinates() {
    ChunkPos pos = new ChunkPos(-1, -2);
    assertEquals(-1, pos.x());
    assertEquals(-2, pos.z());
  }

  @Test
  void packedRoundTrip() {
    ChunkPos original = new ChunkPos(10, 20);
    long packed = original.packed();
    ChunkPos unpacked = ChunkPos.unpack(packed);
    assertEquals(original, unpacked);
  }

  @Test
  void packedRoundTripNegative() {
    ChunkPos original = new ChunkPos(-10, -20);
    long packed = original.packed();
    ChunkPos unpacked = ChunkPos.unpack(packed);
    assertEquals(original, unpacked);
  }

  @Test
  void equalsAndHashCode() {
    ChunkPos a = new ChunkPos(1, 2);
    ChunkPos b = new ChunkPos(1, 2);
    ChunkPos c = new ChunkPos(1, 3);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
  }

  @Test
  void toStringContainsCoordinates() {
    String str = new ChunkPos(1, 2).toString();
    assertTrue(str.contains("1"));
    assertTrue(str.contains("2"));
  }
}
