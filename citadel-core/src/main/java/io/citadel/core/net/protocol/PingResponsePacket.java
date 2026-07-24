package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class PingResponsePacket implements Packet {

  private long payload;

  public PingResponsePacket() {}

  public PingResponsePacket(long payload) {
    this.payload = payload;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x01;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeLong(payload);
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.payload = in.readLong();
  }

  public long getPayload() {
    return payload;
  }
}
