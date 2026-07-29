package io.citadel.core.net.protocol.play;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.world.BlockPos;
import io.citadel.api.world.BlockState;
import io.citadel.api.world.Chunk;
import io.citadel.api.world.ChunkPos;
import io.citadel.core.net.Packet;
import io.citadel.core.net.PacketCodec;
import io.citadel.core.net.PacketFraming;
import io.citadel.core.net.VarInt;
import io.citadel.core.net.protocol.ConfigurationProtocolCodecs;
import io.citadel.core.net.protocol.FinishConfigurationPacket;
import io.citadel.core.net.protocol.KeepAlivePacket;
import io.citadel.core.world.WorldManagerImpl;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LivePacketIntegrationTest {

  private static final UUID BOT_ID = UUID.randomUUID();
  private static final String ACCOUNT_ID = "test-account";

  // ---- Round-trip decode from raw bytes ----

  @Test
  void blockUpdatePacketRoundTrip() throws Exception {
    BlockPos pos = new BlockPos(10, 20, 30);
    int stateId = 42;
    byte[] payload =
        write(
            out -> {
              out.writeLong(pos.packed());
              VarInt.write(stateId, out);
            });
    PacketCodec codec = PlayProtocolCodecs.blockUpdate();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    assertInstanceOf(BlockUpdatePacket.class, packet);
    BlockUpdatePacket bup = (BlockUpdatePacket) packet;
    assertEquals(pos, bup.getPos());
    assertEquals(new BlockState(stateId, 0), bup.getState());
  }

  @Test
  void chunkUnloadPacketRoundTrip() throws Exception {
    int cx = 3;
    int cz = 7;
    byte[] payload =
        write(
            out -> {
              out.writeInt(cx);
              out.writeInt(cz);
            });
    PacketCodec codec = PlayProtocolCodecs.chunkUnload();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    assertInstanceOf(ChunkUnloadPacket.class, packet);
    ChunkUnloadPacket cup = (ChunkUnloadPacket) packet;
    assertEquals(new ChunkPos(cx, cz), cup.getChunkPos());
  }

  @Test
  void multiBlockUpdatePacketRoundTrip() throws Exception {
    byte[] payload =
        write(
            out -> {
              VarInt.write(0, out);
              VarInt.write(0, out);
              VarInt.write(2, out);
              out.writeByte(5);
              out.writeByte(7);
              out.writeShort(10);
              VarInt.write(100, out);
              out.writeByte(3);
              out.writeByte(4);
              out.writeShort(20);
              VarInt.write(200, out);
            });
    PacketCodec codec = PlayProtocolCodecs.multiBlockUpdate();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    assertInstanceOf(MultiBlockUpdatePacket.class, packet);
    MultiBlockUpdatePacket mbup = (MultiBlockUpdatePacket) packet;
    assertEquals(new ChunkPos(0, 0), mbup.getChunkPos());
    assertEquals(2, mbup.getPositions().length);
    assertEquals(new BlockPos(5, 10, 7), mbup.getPositions()[0]);
    assertEquals(new BlockState(100, 0), mbup.getStates()[0]);
    assertEquals(new BlockPos(3, 20, 4), mbup.getPositions()[1]);
    assertEquals(new BlockState(200, 0), mbup.getStates()[1]);
  }

  @Test
  void chunkDataPacketRoundTrip() throws Exception {
    byte[] payload =
        write(
            out -> {
              out.writeInt(5);
              out.writeInt(-3);
              writeEmptyNbt(out);
              VarInt.write(0, out);
              VarInt.write(0, out);
            });
    PacketCodec codec = PlayProtocolCodecs.chunkData();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    assertInstanceOf(ChunkDataPacket.class, packet);
    ChunkDataPacket cdp = (ChunkDataPacket) packet;
    assertEquals(new ChunkPos(5, -3), cdp.getChunkPos());
  }

  @Test
  void keepAlivePacketRoundTrip() throws Exception {
    long id = 12345L;
    byte[] payload = write(out -> out.writeLong(id));
    PacketCodec codec = ConfigurationProtocolCodecs.keepAlive();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    assertInstanceOf(KeepAlivePacket.class, packet);
    assertEquals(id, ((KeepAlivePacket) packet).getId());
  }

  @Test
  void finishConfigurationPacketDecodes() throws Exception {
    byte[] payload = new byte[0];
    PacketCodec codec = ConfigurationProtocolCodecs.finishConfiguration();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    assertInstanceOf(FinishConfigurationPacket.class, packet);
  }

  // ---- WorldManager mutation via handlePacket ----

  @Test
  void blockUpdateMutatesWorld() throws Exception {
    WorldManagerImpl wm = createWorldManager();
    ChunkPos cp = new ChunkPos(0, 0);
    wm.onChunkData(new ChunkDataPacket(cp, allAir()));
    BlockPos pos = new BlockPos(5, 10, 7);
    int stateId = 99;
    byte[] payload =
        write(
            out -> {
              out.writeLong(pos.packed());
              VarInt.write(stateId, out);
            });
    PacketCodec codec = PlayProtocolCodecs.blockUpdate();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    wm.handlePacket(packet);
    assertEquals(new BlockState(stateId, 0), wm.getBlock(pos));
  }

  @Test
  void chunkUnloadRemovesFromWorld() throws Exception {
    WorldManagerImpl wm = createWorldManager();
    ChunkPos cp = new ChunkPos(2, 5);
    wm.onChunkData(new ChunkDataPacket(cp, allAir()));
    assertTrue(wm.isChunkLoaded(cp));
    byte[] payload =
        write(
            out -> {
              out.writeInt(cp.x());
              out.writeInt(cp.z());
            });
    PacketCodec codec = PlayProtocolCodecs.chunkUnload();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    wm.handlePacket(packet);
    assertFalse(wm.isChunkLoaded(cp));
  }

  @Test
  void multiBlockUpdateMutatesWorld() throws Exception {
    WorldManagerImpl wm = createWorldManager();
    ChunkPos cp = new ChunkPos(1, 1);
    wm.onChunkData(new ChunkDataPacket(cp, allAir()));
    byte[] payload =
        write(
            out -> {
              VarInt.write(cp.x(), out);
              VarInt.write(cp.z(), out);
              VarInt.write(1, out);
              out.writeByte(8);
              out.writeByte(3);
              out.writeShort(15);
              VarInt.write(77, out);
            });
    PacketCodec codec = PlayProtocolCodecs.multiBlockUpdate();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    wm.handlePacket(packet);
    assertEquals(new BlockState(77, 0), wm.getBlock(new BlockPos(24, 15, 19)));
  }

  @Test
  void chunkDataLoadsIntoWorld() throws Exception {
    WorldManagerImpl wm = createWorldManager();
    int cx = 4;
    int cz = 6;
    byte[] payload =
        write(
            out -> {
              out.writeInt(cx);
              out.writeInt(cz);
              writeEmptyNbt(out);
              VarInt.write(0, out);
              VarInt.write(0, out);
            });
    PacketCodec codec = PlayProtocolCodecs.chunkData();
    Packet packet = PacketFraming.decode(codec.getPacketId(), payload, codec);
    wm.handlePacket(packet);
    ChunkPos cp = new ChunkPos(cx, cz);
    assertTrue(wm.isChunkLoaded(cp));
    assertNotNull(wm.getChunk(cp));
    assertEquals(BlockState.AIR, wm.getBlock(new BlockPos(cx << 4, 0, cz << 4)));
  }

  // ---- helpers ----

  @FunctionalInterface
  private interface ThrowingConsumer<T> {
    void accept(T t) throws IOException;
  }

  private static byte[] write(ThrowingConsumer<DataOutputStream> writer) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(buf);
    writer.accept(out);
    out.flush();
    return buf.toByteArray();
  }

  private static void writeEmptyNbt(DataOutputStream out) throws IOException {
    out.writeByte(0x0A);
    out.writeShort(0);
    out.writeByte(0);
  }

  private static BlockState[] allAir() {
    BlockState[] blocks = new BlockState[Chunk.BLOCK_COUNT];
    for (int i = 0; i < blocks.length; i++) {
      blocks[i] = BlockState.AIR;
    }
    return blocks;
  }

  private static WorldManagerImpl createWorldManager() {
    return new WorldManagerImpl(BOT_ID, ACCOUNT_ID, new RecordingEventBus(), silentLogger());
  }

  private static io.citadel.api.service.Logger silentLogger() {
    return new io.citadel.api.service.Logger() {
      @Override
      public boolean isTraceEnabled() {
        return false;
      }

      @Override
      public boolean isDebugEnabled() {
        return false;
      }

      @Override
      public boolean isInfoEnabled() {
        return false;
      }

      @Override
      public boolean isWarnEnabled() {
        return false;
      }

      @Override
      public boolean isErrorEnabled() {
        return false;
      }

      @Override
      public void trace(String message) {}

      @Override
      public void trace(String format, Object... args) {}

      @Override
      public void trace(String message, Throwable throwable) {}

      @Override
      public void trace(String accountName, String message) {}

      @Override
      public void trace(String accountName, String format, Object... args) {}

      @Override
      public void trace(String accountName, String message, Throwable throwable) {}

      @Override
      public void debug(String message) {}

      @Override
      public void debug(String format, Object... args) {}

      @Override
      public void debug(String message, Throwable throwable) {}

      @Override
      public void debug(String accountName, String message) {}

      @Override
      public void debug(String accountName, String format, Object... args) {}

      @Override
      public void debug(String accountName, String message, Throwable throwable) {}

      @Override
      public void info(String message) {}

      @Override
      public void info(String format, Object... args) {}

      @Override
      public void info(String message, Throwable throwable) {}

      @Override
      public void info(String accountName, String message) {}

      @Override
      public void info(String accountName, String format, Object... args) {}

      @Override
      public void info(String accountName, String message, Throwable throwable) {}

      @Override
      public void warn(String message) {}

      @Override
      public void warn(String format, Object... args) {}

      @Override
      public void warn(String message, Throwable throwable) {}

      @Override
      public void warn(String accountName, String message) {}

      @Override
      public void warn(String accountName, String format, Object... args) {}

      @Override
      public void warn(String accountName, String message, Throwable throwable) {}

      @Override
      public void error(String message) {}

      @Override
      public void error(String format, Object... args) {}

      @Override
      public void error(String message, Throwable throwable) {}

      @Override
      public void error(String accountName, String message) {}

      @Override
      public void error(String accountName, String format, Object... args) {}

      @Override
      public void error(String accountName, String message, Throwable throwable) {}
    };
  }

  private static final class RecordingEventBus implements io.citadel.api.event.EventBus {
    private final java.util.List<io.citadel.api.event.Event> eventList =
        new java.util.concurrent.CopyOnWriteArrayList<>();

    java.util.List<io.citadel.api.event.Event> events() {
      return java.util.List.copyOf(eventList);
    }

    @Override
    public <T extends io.citadel.api.event.Event> io.citadel.api.event.Subscription subscribe(
        Class<T> type, io.citadel.api.event.EventHandler<T> handler) {
      return () -> {};
    }

    @Override
    public <T extends io.citadel.api.event.Event> void unsubscribe(
        Class<T> type, io.citadel.api.event.EventHandler<T> handler) {}

    @Override
    public void publish(io.citadel.api.event.Event event) {
      eventList.add(event);
    }

    @Override
    public void publishAsync(io.citadel.api.event.Event event) {
      eventList.add(event);
    }
  }
}
