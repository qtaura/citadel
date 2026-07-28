package io.citadel.api.world;

public record BlockPos(int x, int y, int z) {

  public BlockPos {
    if (y < -1024 || y > 1024) {
      throw new IllegalArgumentException("y out of range: " + y);
    }
  }

  public BlockPos north() {
    return new BlockPos(x, y, z - 1);
  }

  public BlockPos south() {
    return new BlockPos(x, y, z + 1);
  }

  public BlockPos east() {
    return new BlockPos(x + 1, y, z);
  }

  public BlockPos west() {
    return new BlockPos(x - 1, y, z);
  }

  public BlockPos above() {
    return new BlockPos(x, y + 1, z);
  }

  public BlockPos below() {
    return new BlockPos(x, y - 1, z);
  }

  public ChunkPos chunkPos() {
    return new ChunkPos(x >> 4, z >> 4);
  }

  public long packed() {
    return (x & 0x3FFFFFFL) << 38 | (z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
  }

  public static BlockPos unpack(long packed) {
    int x = (int) (packed >> 38);
    int y = (int) (packed << 52 >> 52);
    int z = (int) (packed << 26 >> 38);
    return new BlockPos(x, y, z);
  }

  @Override
  public String toString() {
    return "BlockPos{x=" + x + ", y=" + y + ", z=" + z + "}";
  }
}
