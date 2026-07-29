package io.citadel.core.net.protocol.play;

import io.citadel.api.network.ProtocolState;
import io.citadel.api.world.ChunkPos;
import io.citadel.core.net.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class ChunkUnloadPacket implements Packet {

  private ChunkPos chunkPos;

  public ChunkUnloadPacket() {}

  public ChunkUnloadPacket(ChunkPos chunkPos) {
    this.chunkPos = chunkPos;
  }

  public ChunkPos getChunkPos() {
    return chunkPos;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x1E;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void read(DataInput in) throws IOException {
    int cx = in.readInt();
    int cz = in.readInt();
    this.chunkPos = new ChunkPos(cx, cz);
  }
}
