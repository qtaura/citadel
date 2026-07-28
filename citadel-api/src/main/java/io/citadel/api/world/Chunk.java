package io.citadel.api.world;

import java.util.Arrays;

public final class Chunk {

  public static final int SIZE_X = 16;
  public static final int SIZE_Y = 256;
  public static final int SIZE_Z = 16;
  public static final int BLOCK_COUNT = SIZE_X * SIZE_Y * SIZE_Z;

  private final ChunkPos position;
  private final BlockState[] blocks;

  public Chunk(ChunkPos position, BlockState[] blocks) {
    this.position = position;
    if (blocks.length != BLOCK_COUNT) {
      throw new IllegalArgumentException(
          "Expected " + BLOCK_COUNT + " blocks, got " + blocks.length);
    }
    this.blocks = blocks.clone();
  }

  public ChunkPos getPosition() {
    return position;
  }

  public BlockState getBlock(int x, int y, int z) {
    if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
      throw new IndexOutOfBoundsException(
          "Local coordinates out of range: (" + x + ", " + y + ", " + z + ")");
    }
    return blocks[index(x, y, z)];
  }

  public BlockState getBlock(BlockPos pos) {
    return getBlock(pos.x() & 0xF, pos.y(), pos.z() & 0xF);
  }

  public BlockState[] getBlocks() {
    return blocks.clone();
  }

  public int nonAirCount() {
    int count = 0;
    for (BlockState block : blocks) {
      if (!block.isAir()) {
        count++;
      }
    }
    return count;
  }

  public static int index(int x, int y, int z) {
    return (y << 8) | (z << 4) | x;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Chunk other)) {
      return false;
    }
    return position.equals(other.position) && Arrays.equals(blocks, other.blocks);
  }

  @Override
  public int hashCode() {
    int result = position.hashCode();
    result = 31 * result + Arrays.hashCode(blocks);
    return result;
  }

  @Override
  public String toString() {
    return "Chunk{position=" + position + ", nonAir=" + nonAirCount() + "}";
  }
}
