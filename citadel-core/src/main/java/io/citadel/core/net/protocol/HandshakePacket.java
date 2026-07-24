package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    byte[] addrBytes = serverAddress.getBytes(StandardCharsets.UTF_8);
    VarInt.write(addrBytes.length, out);
    out.write(addrBytes);
    out.writeShort(serverPort);
    VarInt.write(nextState, out);
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.protocolVersion = VarInt.read(in);
    int addrLen = VarInt.read(in);
    byte[] addrBytes = new byte[addrLen];
    in.readFully(addrBytes);
    this.serverAddress = new String(addrBytes, StandardCharsets.UTF_8);
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
