package io.citadel.core.net.protocol.play;

import io.citadel.api.network.ProtocolState;
import io.citadel.api.world.BlockState;
import io.citadel.api.world.Chunk;
import io.citadel.api.world.ChunkPos;
import io.citadel.core.net.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class ChunkDataPacket implements Packet {

  private final ChunkPos chunkPos;
  private final BlockState[] blocks;

  public ChunkDataPacket(ChunkPos chunkPos, BlockState[] blocks) {
    if (blocks.length != Chunk.BLOCK_COUNT) {
      throw new IllegalArgumentException(
          "Expected " + Chunk.BLOCK_COUNT + " blocks, got " + blocks.length);
    }
    this.chunkPos = chunkPos;
    this.blocks = blocks.clone();
  }

  public ChunkPos getChunkPos() {
    return chunkPos;
  }

  public BlockState[] getBlocks() {
    return blocks.clone();
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x20;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void read(DataInput in) throws IOException {
    throw new UnsupportedOperationException();
  }
}
