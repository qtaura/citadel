package io.citadel.core.net.protocol;

import io.citadel.core.net.Packet;
import io.citadel.core.net.PacketCodec;

public final class ConfigurationProtocolCodecs {

  private ConfigurationProtocolCodecs() {}

  public static PacketCodec finishConfiguration() {
    return codec(0x02, FinishConfigurationPacket::new);
  }

  public static PacketCodec keepAlive() {
    return codec(0x03, KeepAlivePacket::new);
  }

  private static PacketCodec codec(int packetId, PacketFactory factory) {
    return new PacketCodec() {
      @Override
      public int getPacketId() {
        return packetId;
      }

      @Override
      public Packet create() {
        return factory.create();
      }
    };
  }

  private interface PacketFactory {
    Packet create();
  }
}
