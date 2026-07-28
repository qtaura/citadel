package io.citadel.api.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BlockPosTest {

  @Test
  void constructorStoresCoordinates() {
    BlockPos pos = new BlockPos(1, 2, 3);
    assertEquals(1, pos.x());
    assertEquals(2, pos.y());
    assertEquals(3, pos.z());
  }

  @Test
  void negativeCoordinates() {
    BlockPos pos = new BlockPos(-10, 0, -20);
    assertEquals(-10, pos.x());
    assertEquals(0, pos.y());
    assertEquals(-20, pos.z());
  }

  @Test
  void yOutOfRangeThrows() {
    assertThrows(IllegalArgumentException.class, () -> new BlockPos(0, -1025, 0));
    assertThrows(IllegalArgumentException.class, () -> new BlockPos(0, 1025, 0));
  }

  @Test
  void northDecreasesZ() {
    assertEquals(new BlockPos(0, 0, -1), new BlockPos(0, 0, 0).north());
  }

  @Test
  void southIncreasesZ() {
    assertEquals(new BlockPos(0, 0, 1), new BlockPos(0, 0, 0).south());
  }

  @Test
  void eastIncreasesX() {
    assertEquals(new BlockPos(1, 0, 0), new BlockPos(0, 0, 0).east());
  }

  @Test
  void westDecreasesX() {
    assertEquals(new BlockPos(-1, 0, 0), new BlockPos(0, 0, 0).west());
  }

  @Test
  void aboveIncreasesY() {
    assertEquals(new BlockPos(0, 1, 0), new BlockPos(0, 0, 0).above());
  }

  @Test
  void belowDecreasesY() {
    assertEquals(new BlockPos(0, -1, 0), new BlockPos(0, 0, 0).below());
  }

  @Test
  void chunkPosDerivation() {
    assertEquals(new ChunkPos(0, 0), new BlockPos(0, 0, 0).chunkPos());
    assertEquals(new ChunkPos(0, 0), new BlockPos(15, 0, 15).chunkPos());
    assertEquals(new ChunkPos(1, 1), new BlockPos(16, 0, 16).chunkPos());
    assertEquals(new ChunkPos(-1, -1), new BlockPos(-1, 0, -1).chunkPos());
  }

  @Test
  void packedRoundTrip() {
    BlockPos original = new BlockPos(10, 20, 30);
    long packed = original.packed();
    BlockPos unpacked = BlockPos.unpack(packed);
    assertEquals(original, unpacked);
  }

  @Test
  void packedRoundTripNegative() {
    BlockPos original = new BlockPos(-10, -5, -30);
    long packed = original.packed();
    BlockPos unpacked = BlockPos.unpack(packed);
    assertEquals(original, unpacked);
  }

  @Test
  void packedRoundTripExtremes() {
    BlockPos original = new BlockPos(0x1FFFFFF, 511, 0x1FFFFFF);
    long packed = original.packed();
    BlockPos unpacked = BlockPos.unpack(packed);
    assertEquals(original, unpacked);
  }

  @Test
  void equalsAndHashCode() {
    BlockPos a = new BlockPos(1, 2, 3);
    BlockPos b = new BlockPos(1, 2, 3);
    BlockPos c = new BlockPos(1, 2, 4);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
  }

  @Test
  void toStringContainsCoordinates() {
    String str = new BlockPos(1, 2, 3).toString();
    assertTrue(str.contains("1"));
    assertTrue(str.contains("2"));
    assertTrue(str.contains("3"));
  }
}
