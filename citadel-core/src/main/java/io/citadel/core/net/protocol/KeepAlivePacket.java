package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class KeepAlivePacket implements Packet {

  private long id;

  public KeepAlivePacket() {}

  public KeepAlivePacket(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x03;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeLong(id);
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.id = in.readLong();
  }
}
