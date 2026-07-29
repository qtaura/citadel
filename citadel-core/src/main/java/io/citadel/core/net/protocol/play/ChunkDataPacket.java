package io.citadel.core.net.protocol.play;

import io.citadel.api.network.ProtocolState;
import io.citadel.api.world.BlockState;
import io.citadel.api.world.Chunk;
import io.citadel.api.world.ChunkPos;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class ChunkDataPacket implements Packet {

  private ChunkPos chunkPos;
  private BlockState[] blocks;

  public ChunkDataPacket() {}

  public ChunkDataPacket(ChunkPos chunkPos, BlockState[] blocks) {
    if (blocks.length != Chunk.BLOCK_COUNT) {
      throw new IllegalArgumentException(
          "Expected " + Chunk.BLOCK_COUNT + " blocks, got " + blocks.length);
    }
    this.chunkPos = chunkPos;
    this.blocks = blocks.clone();
  }

  public ChunkPos getChunkPos() {
    return chunkPos;
  }

  public BlockState[] getBlocks() {
    return blocks.clone();
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x25;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void read(DataInput in) throws IOException {
    int cx = in.readInt();
    int cz = in.readInt();
    this.chunkPos = new ChunkPos(cx, cz);
    skipNbt(in);
    int dataSize = VarInt.read(in);
    in.skipBytes(dataSize);
    int blockEntityCount = VarInt.read(in);
    for (int i = 0; i < blockEntityCount; i++) {
      skipNbt(in);
    }
    this.blocks = allAir();
  }

  private static void skipNbt(DataInput in) throws IOException {
    int tagType = in.readUnsignedByte();
    if (tagType == 0) {
      return;
    }
    int nameLength = in.readUnsignedShort();
    if (nameLength > 0) {
      in.skipBytes(nameLength);
    }
    skipTag(in, tagType);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private static void skipTag(DataInput in, int tagType) throws IOException {
    switch (tagType) {
      case 1 -> in.readByte();
      case 2 -> in.readShort();
      case 3 -> in.readInt();
      case 4 -> in.readLong();
      case 5 -> in.readFloat();
      case 6 -> in.readDouble();
      case 7 -> {
        int len = in.readInt();
        in.skipBytes(len);
      }
      case 8 -> {
        int len = in.readUnsignedShort();
        in.skipBytes(len);
      }
      case 9 -> {
        int elementType = in.readUnsignedByte();
        int elementCount = in.readInt();
        for (int i = 0; i < elementCount; i++) {
          skipTag(in, elementType);
        }
      }
      case 10 -> {
        while (true) {
          int subType = in.readUnsignedByte();
          if (subType == 0) {
            return;
          }
          int subNameLen = in.readUnsignedShort();
          if (subNameLen > 0) {
            in.skipBytes(subNameLen);
          }
          skipTag(in, subType);
        }
      }
      case 11 -> {
        int len = in.readInt();
        in.skipBytes(len * 4);
      }
      case 12 -> {
        int len = in.readInt();
        in.skipBytes(len * 8);
      }
      default -> throw new IOException("Unknown NBT tag type: " + tagType);
    }
  }

  private static BlockState[] allAir() {
    BlockState[] blocks = new BlockState[Chunk.BLOCK_COUNT];
    for (int i = 0; i < blocks.length; i++) {
      blocks[i] = BlockState.AIR;
    }
    return blocks;
  }
}
