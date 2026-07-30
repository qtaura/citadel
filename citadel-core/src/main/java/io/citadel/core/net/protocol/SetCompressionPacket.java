package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class SetCompressionPacket implements Packet {

  private int threshold;

  public int getThreshold() {
    return threshold;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x03;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.threshold = VarInt.read(in);
  }
}
