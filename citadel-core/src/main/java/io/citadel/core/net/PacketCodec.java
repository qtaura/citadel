package io.citadel.core.net;

public interface PacketCodec {

  int getPacketId();

  Packet create();
}
