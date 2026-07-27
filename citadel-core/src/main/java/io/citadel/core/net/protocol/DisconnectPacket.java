package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.MinecraftStrings;
import io.citadel.core.net.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class DisconnectPacket implements Packet {

  private String reasonJson;

  public DisconnectPacket() {}

  public DisconnectPacket(String reasonJson) {
    this.reasonJson = reasonJson;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x00;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    MinecraftStrings.write(reasonJson, out);
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.reasonJson = MinecraftStrings.read(in);
  }

  public String getReasonJson() {
    return reasonJson;
  }
}
