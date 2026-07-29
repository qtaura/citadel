package io.citadel.core.net.protocol.play;

import io.citadel.api.network.ProtocolState;
import io.citadel.api.world.BlockPos;
import io.citadel.api.world.BlockState;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class BlockUpdatePacket implements Packet {

  private BlockPos pos;
  private BlockState state;

  public BlockUpdatePacket() {}

  public BlockUpdatePacket(BlockPos pos, BlockState state) {
    this.pos = pos;
    this.state = state;
  }

  public BlockPos getPos() {
    return pos;
  }

  public BlockState getState() {
    return state;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x0C;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void read(DataInput in) throws IOException {
    long packedPos = in.readLong();
    int stateId = VarInt.read(in);
    this.pos = BlockPos.unpack(packedPos);
    this.state = new BlockState(stateId, 0);
  }
}
