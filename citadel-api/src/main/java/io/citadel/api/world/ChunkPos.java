package io.citadel.api.world;

public record ChunkPos(int x, int z) {

  public long packed() {
    return (long) x << 32 | (z & 0xFFFFFFFFL);
  }

  public static long packed(int x, int z) {
    return (long) x << 32 | (z & 0xFFFFFFFFL);
  }

  public static ChunkPos unpack(long packed) {
    return new ChunkPos((int) (packed >> 32), (int) packed);
  }

  @Override
  public String toString() {
    return "ChunkPos{x=" + x + ", z=" + z + "}";
  }
}
