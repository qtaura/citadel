package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class StatusResponsePacket implements Packet {

  private String json;

  public StatusResponsePacket() {}

  public StatusResponsePacket(String json) {
    this.json = json;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x00;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
    VarInt.write(jsonBytes.length, out);
    out.write(jsonBytes);
  }

  @Override
  public void read(DataInput in) throws IOException {
    int len = VarInt.read(in);
    byte[] buf = new byte[len];
    in.readFully(buf);
    this.json = new String(buf, StandardCharsets.UTF_8);
  }

  public String getJson() {
    return json;
  }
}
