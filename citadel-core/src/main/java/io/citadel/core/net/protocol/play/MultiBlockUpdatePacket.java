package io.citadel.core.net.protocol.play;

import io.citadel.api.network.ProtocolState;
import io.citadel.api.world.BlockPos;
import io.citadel.api.world.BlockState;
import io.citadel.api.world.ChunkPos;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class MultiBlockUpdatePacket implements Packet {

  private ChunkPos chunkPos;
  private BlockPos[] positions;
  private BlockState[] states;

  public MultiBlockUpdatePacket() {}

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
    int cx = VarInt.read(in);
    int cz = VarInt.read(in);
    this.chunkPos = new ChunkPos(cx, cz);
    int count = VarInt.read(in);
    this.positions = new BlockPos[count];
    this.states = new BlockState[count];
    for (int i = 0; i < count; i++) {
      int lx = in.readUnsignedByte();
      int lz = in.readUnsignedByte();
      int y = in.readShort();
      int stateId = VarInt.read(in);
      positions[i] = new BlockPos((cx << 4) | (lx & 0xF), y, (cz << 4) | (lz & 0xF));
      states[i] = new BlockState(stateId, 0);
    }
  }
}
