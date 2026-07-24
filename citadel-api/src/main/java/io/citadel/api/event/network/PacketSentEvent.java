package io.citadel.api.event.network;

import io.citadel.api.event.Event;
import io.citadel.api.network.ProtocolState;

public final class PacketSentEvent extends Event {

  private final int packetId;
  private final ProtocolState protocolState;
  private final String remoteAddress;
  private final int remotePort;
  private final int payloadSize;

  public PacketSentEvent(
      int packetId,
      ProtocolState protocolState,
      String remoteAddress,
      int remotePort,
      int payloadSize) {
    this.packetId = packetId;
    this.protocolState = protocolState;
    this.remoteAddress = remoteAddress;
    this.remotePort = remotePort;
    this.payloadSize = payloadSize;
  }

  public int getPacketId() {
    return packetId;
  }

  public ProtocolState getProtocolState() {
    return protocolState;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public int getRemotePort() {
    return remotePort;
  }

  public int getPayloadSize() {
    return payloadSize;
  }
}
