package io.citadel.core.net.protocol;

import io.citadel.core.net.Packet;
import io.citadel.core.net.PacketCodec;

public final class LoginProtocolCodecs {

  private LoginProtocolCodecs() {}

  public static PacketCodec disconnect() {
    return codec(0x00, DisconnectPacket::new);
  }

  public static PacketCodec encryptionRequest() {
    return codec(0x01, EncryptionRequestPacket::new);
  }

  public static PacketCodec loginSuccess() {
    return codec(0x02, LoginSuccessPacket::new);
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
