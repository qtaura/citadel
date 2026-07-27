package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.MinecraftStrings;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class HandshakePacket implements Packet {

  private int protocolVersion;
  private String serverAddress;
  private int serverPort;
  private int nextState;

  public HandshakePacket() {}

  public HandshakePacket(int protocolVersion, String serverAddress, int serverPort, int nextState) {
    this.protocolVersion = protocolVersion;
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
    this.nextState = nextState;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x00;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    VarInt.write(protocolVersion, out);
    MinecraftStrings.write(serverAddress, out);
    out.writeShort(serverPort);
    VarInt.write(nextState, out);
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.protocolVersion = VarInt.read(in);
    this.serverAddress = MinecraftStrings.read(in);
    this.serverPort = in.readUnsignedShort();
    this.nextState = VarInt.read(in);
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public int getServerPort() {
    return serverPort;
  }

  public int getNextState() {
    return nextState;
  }
}
