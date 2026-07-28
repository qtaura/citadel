package io.citadel.api.world;

import java.util.List;

public interface WorldManager {

  BlockState getBlock(BlockPos position);

  BlockState getBlock(int x, int y, int z);

  Chunk getChunk(ChunkPos position);

  boolean isChunkLoaded(ChunkPos position);

  List<Chunk> getLoadedChunks();

  int loadedChunkCount();

  void clear();
}
