package io.citadel.core.net.protocol;

import io.citadel.core.net.Packet;
import io.citadel.core.net.PacketCodec;

public final class StatusProtocolCodecs {

  private StatusProtocolCodecs() {}

  public static PacketCodec statusRequest() {
    return new PacketCodec() {
      @Override
      public int getPacketId() {
        return 0x00;
      }

      @Override
      public Packet create() {
        return new StatusRequestPacket();
      }
    };
  }

  public static PacketCodec statusResponse() {
    return new PacketCodec() {
      @Override
      public int getPacketId() {
        return 0x00;
      }

      @Override
      public Packet create() {
        return new StatusResponsePacket();
      }
    };
  }

  public static PacketCodec pingRequest() {
    return new PacketCodec() {
      @Override
      public int getPacketId() {
        return 0x01;
      }

      @Override
      public Packet create() {
        return new PingRequestPacket();
      }
    };
  }

  public static PacketCodec pingResponse() {
    return new PacketCodec() {
      @Override
      public int getPacketId() {
        return 0x01;
      }

      @Override
      public Packet create() {
        return new PingResponsePacket();
      }
    };
  }
}
