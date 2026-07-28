package io.citadel.core.net.protocol.play;

import io.citadel.api.network.ProtocolState;
import io.citadel.api.world.BlockPos;
import io.citadel.api.world.BlockState;
import io.citadel.api.world.ChunkPos;
import io.citadel.core.net.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class MultiBlockUpdatePacket implements Packet {

  private final ChunkPos chunkPos;
  private final BlockPos[] positions;
  private final BlockState[] states;

  public MultiBlockUpdatePacket(ChunkPos chunkPos, BlockPos[] positions, BlockState[] states) {
    if (positions.length != states.length) {
      throw new IllegalArgumentException("positions and states must have same length");
    }
    this.chunkPos = chunkPos;
    this.positions = positions.clone();
    this.states = states.clone();
  }

  public ChunkPos getChunkPos() {
    return chunkPos;
  }

  public BlockPos[] getPositions() {
    return positions.clone();
  }

  public BlockState[] getStates() {
    return states.clone();
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x3A;
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
