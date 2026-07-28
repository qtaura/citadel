package io.citadel.api.world;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Chunk {

  public static final int SIZE_X = 16;
  public static final int SIZE_Y = 256;
  public static final int SIZE_Z = 16;
  public static final int BLOCK_COUNT = SIZE_X * SIZE_Y * SIZE_Z;

  private static final int MIN_BITS = 4;

  private final ChunkPos position;
  private final BlockState[] palette;
  private final long[] storage;
  private final int bitsPerEntry;
  private final int nonAirBlocks;
  private final int hash;

  public Chunk(ChunkPos position, BlockState[] blocks) {
    this.position = position;
    if (blocks.length != BLOCK_COUNT) {
      throw new IllegalArgumentException(
          "Expected " + BLOCK_COUNT + " blocks, got " + blocks.length);
    }
    BlockState[] blocksCopy = blocks.clone();
    Map<BlockState, Integer> paletteMap = new HashMap<>();
    int[] tempIndices = new int[BLOCK_COUNT];
    int nonAir = 0;
    int h = position.hashCode();
    for (int i = 0; i < BLOCK_COUNT; i++) {
      BlockState state = blocksCopy[i];
      if (!state.isAir()) {
        nonAir++;
      }
      int idx = paletteMap.computeIfAbsent(state, k -> paletteMap.size());
      tempIndices[i] = idx;
      h = 31 * h + state.hashCode();
    }
    this.nonAirBlocks = nonAir;
    this.hash = h;
    this.palette = new BlockState[paletteMap.size()];
    paletteMap.forEach((state, idx) -> palette[idx] = state);
    int paletteSize = palette.length;
    if (paletteSize <= 1) {
      this.bitsPerEntry = 0;
      this.storage = new long[0];
    } else {
      this.bitsPerEntry = Math.max(MIN_BITS, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
      this.storage = new long[(BLOCK_COUNT * bitsPerEntry + 63) / 64];
      for (int i = 0; i < BLOCK_COUNT; i++) {
        setIndex(i, tempIndices[i]);
      }
    }
  }

  public ChunkPos getPosition() {
    return position;
  }

  public BlockState getBlock(int x, int y, int z) {
    if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
      throw new IndexOutOfBoundsException(
          "Local coordinates out of range: (" + x + ", " + y + ", " + z + ")");
    }
    return palette[readIndex(index(x, y, z))];
  }

  public BlockState getBlock(BlockPos pos) {
    return getBlock(pos.x() & 0xF, pos.y(), pos.z() & 0xF);
  }

  public BlockState[] getBlocks() {
    BlockState[] copy = new BlockState[BLOCK_COUNT];
    for (int i = 0; i < BLOCK_COUNT; i++) {
      copy[i] = palette[readIndex(i)];
    }
    return copy;
  }

  public int nonAirCount() {
    return nonAirBlocks;
  }

  public static int index(int x, int y, int z) {
    return (y << 8) | (z << 4) | x;
  }

  private int readIndex(int idx) {
    if (bitsPerEntry == 0) {
      return 0;
    }
    int bitIndex = idx * bitsPerEntry;
    int longIndex = bitIndex >> 6;
    int offset = bitIndex & 63;
    if (offset + bitsPerEntry <= 64) {
      return (int) ((storage[longIndex] >>> offset) & ((1L << bitsPerEntry) - 1));
    }
    int lowBits = 64 - offset;
    long val = (storage[longIndex] >>> offset) & ((1L << lowBits) - 1);
    val |= (storage[longIndex + 1] & ((1L << (bitsPerEntry - lowBits)) - 1)) << lowBits;
    return (int) val;
  }

  private void setIndex(int idx, int index) {
    int bitIndex = idx * bitsPerEntry;
    int longIndex = bitIndex >> 6;
    int offset = bitIndex & 63;
    long mask = (1L << bitsPerEntry) - 1;
    if (offset + bitsPerEntry <= 64) {
      storage[longIndex] = (storage[longIndex] & ~(mask << offset)) | ((long) index << offset);
    } else {
      int lowBits = 64 - offset;
      storage[longIndex] =
          (storage[longIndex] & ~(mask << offset)) | ((index & ((1L << lowBits) - 1)) << offset);
      storage[longIndex + 1] = (storage[longIndex + 1] & ~(mask >>> lowBits)) | (index >>> lowBits);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Chunk other)) {
      return false;
    }
    if (!position.equals(other.position)) {
      return false;
    }
    if (nonAirBlocks != other.nonAirBlocks) {
      return false;
    }
    if (hash != other.hash) {
      return false;
    }
    if (palette.length == 1 && other.palette.length == 1) {
      return palette[0].equals(other.palette[0]);
    }
    return Arrays.equals(getBlocks(), other.getBlocks());
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return "Chunk{position=" + position + ", nonAir=" + nonAirBlocks + "}";
  }
}
