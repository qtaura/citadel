package io.citadel.core.net.protocol.play;

import io.citadel.core.net.Packet;
import io.citadel.core.net.PacketCodec;

public final class PlayProtocolCodecs {

  private PlayProtocolCodecs() {}

  public static PacketCodec blockUpdate() {
    return codec(0x0C, BlockUpdatePacket::new);
  }

  public static PacketCodec chunkUnload() {
    return codec(0x1E, ChunkUnloadPacket::new);
  }

  public static PacketCodec chunkData() {
    return codec(0x25, ChunkDataPacket::new);
  }

  public static PacketCodec multiBlockUpdate() {
    return codec(0x3A, MultiBlockUpdatePacket::new);
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
