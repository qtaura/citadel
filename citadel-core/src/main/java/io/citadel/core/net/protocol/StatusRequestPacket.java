package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class StatusRequestPacket implements Packet {

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x00;
  }

  @Override
  public void write(DataOutput out) throws IOException {}

  @Override
  public void read(DataInput in) throws IOException {}
}
