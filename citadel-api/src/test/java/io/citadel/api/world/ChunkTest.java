package io.citadel.api.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChunkTest {

  @Test
  void constructorStoresPosition() {
    ChunkPos pos = new ChunkPos(1, 2);
    BlockState[] blocks = new BlockState[Chunk.BLOCK_COUNT];
    for (int i = 0; i < blocks.length; i++) {
      blocks[i] = BlockState.AIR;
    }
    Chunk chunk = new Chunk(pos, blocks);
    assertEquals(pos, chunk.getPosition());
  }

  @Test
  void wrongBlockCountThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> new Chunk(new ChunkPos(0, 0), new BlockState[10]));
  }

  @Test
  void getBlockReturnsCorrectState() {
    ChunkPos pos = new ChunkPos(0, 0);
    BlockState[] blocks = new BlockState[Chunk.BLOCK_COUNT];
    for (int i = 0; i < blocks.length; i++) {
      blocks[i] = BlockState.AIR;
    }
    BlockState stone = new BlockState(1, 0);
    blocks[Chunk.index(5, 10, 7)] = stone;
    Chunk chunk = new Chunk(pos, blocks);
    assertEquals(stone, chunk.getBlock(5, 10, 7));
    assertEquals(BlockState.AIR, chunk.getBlock(0, 0, 0));
  }

  @Test
  void getBlockByBlockPos() {
    ChunkPos pos = new ChunkPos(1, 2);
    BlockState[] blocks = new BlockState[Chunk.BLOCK_COUNT];
    for (int i = 0; i < blocks.length; i++) {
      blocks[i] = BlockState.AIR;
    }
    BlockState stone = new BlockState(1, 0);
    blocks[Chunk.index(3, 20, 12)] = stone;
    Chunk chunk = new Chunk(pos, blocks);
    BlockPos worldPos = new BlockPos(1 * 16 + 3, 20, 2 * 16 + 12);
    assertEquals(stone, chunk.getBlock(worldPos));
  }

  @Test
  void getBlockOutOfRangeThrows() {
    Chunk chunk = new Chunk(new ChunkPos(0, 0), allAir());
    assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(-1, 0, 0));
    assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(16, 0, 0));
    assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, -1, 0));
    assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, 256, 0));
    assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, 0, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, 0, 16));
  }

  @Test
  void nonAirCount() {
    BlockState[] blocks = allAir();
    blocks[Chunk.index(0, 0, 0)] = new BlockState(1, 0);
    blocks[Chunk.index(1, 0, 0)] = new BlockState(2, 0);
    Chunk chunk = new Chunk(new ChunkPos(0, 0), blocks);
    assertEquals(2, chunk.nonAirCount());
  }

  @Test
  void nonAirCountEmpty() {
    Chunk chunk = new Chunk(new ChunkPos(0, 0), allAir());
    assertEquals(0, chunk.nonAirCount());
  }

  @Test
  void getBlocksReturnsCopy() {
    Chunk chunk = new Chunk(new ChunkPos(0, 0), allAir());
    BlockState[] copy = chunk.getBlocks();
    assertNotNull(copy);
    assertEquals(Chunk.BLOCK_COUNT, copy.length);
    copy[0] = new BlockState(99, 0);
    assertEquals(BlockState.AIR, chunk.getBlock(0, 0, 0));
  }

  @Test
  void constructorCopiesInput() {
    BlockState[] blocks = allAir();
    blocks[Chunk.index(0, 0, 0)] = new BlockState(1, 0);
    Chunk chunk = new Chunk(new ChunkPos(0, 0), blocks);
    blocks[Chunk.index(0, 0, 0)] = BlockState.AIR;
    assertEquals(new BlockState(1, 0), chunk.getBlock(0, 0, 0));
  }

  @Test
  void equalsAndHashCode() {
    ChunkPos pos = new ChunkPos(0, 0);
    BlockState[] blocks = allAir();
    blocks[Chunk.index(0, 0, 0)] = new BlockState(1, 0);
    Chunk a = new Chunk(pos, blocks);
    Chunk b = new Chunk(pos, blocks);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void toStringContainsPosition() {
    Chunk chunk = new Chunk(new ChunkPos(5, 10), allAir());
    String str = chunk.toString();
    assertTrue(str.contains("5"));
    assertTrue(str.contains("10"));
  }

  private static BlockState[] allAir() {
    BlockState[] blocks = new BlockState[Chunk.BLOCK_COUNT];
    for (int i = 0; i < blocks.length; i++) {
      blocks[i] = BlockState.AIR;
    }
    return blocks;
  }
}
