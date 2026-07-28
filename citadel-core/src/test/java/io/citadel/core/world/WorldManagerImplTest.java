package io.citadel.core.world;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.EventHandler;
import io.citadel.api.event.Subscription;
import io.citadel.api.event.world.BlockUpdatedEvent;
import io.citadel.api.event.world.ChunkLoadedEvent;
import io.citadel.api.event.world.ChunkUnloadedEvent;
import io.citadel.api.event.world.WorldClearedEvent;
import io.citadel.api.service.Logger;
import io.citadel.api.world.BlockPos;
import io.citadel.api.world.BlockState;
import io.citadel.api.world.Chunk;
import io.citadel.api.world.ChunkPos;
import io.citadel.core.net.protocol.play.BlockUpdatePacket;
import io.citadel.core.net.protocol.play.ChunkDataPacket;
import io.citadel.core.net.protocol.play.ChunkUnloadPacket;
import io.citadel.core.net.protocol.play.MultiBlockUpdatePacket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.AvoidInstantiatingObjectsInLoops",
  "PMD.GodClass",
  "PMD.CouplingBetweenObjects"
})
class WorldManagerImplTest {

  private static final UUID BOT_ID = UUID.randomUUID();
  private static final String ACCOUNT_ID = "test-account";

  @Test
  void getBlockReturnsAirForUnloadedChunk() {
    WorldManagerImpl wm = createManager();
    assertEquals(BlockState.AIR, wm.getBlock(new BlockPos(0, 0, 0)));
  }

  @Test
  void getBlockAfterChunkLoad() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0, new BlockState(1, 0), 0, 0, 0);
    assertEquals(new BlockState(1, 0), wm.getBlock(new BlockPos(0, 0, 0)));
  }

  @Test
  void getBlockInAdjacentChunk() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 1, 0, new BlockState(1, 0), 0, 50, 0);
    assertEquals(new BlockState(1, 0), wm.getBlock(new BlockPos(16, 50, 0)));
    assertEquals(BlockState.AIR, wm.getBlock(new BlockPos(15, 50, 0)));
  }

  @Test
  void isChunkLoaded() {
    WorldManagerImpl wm = createManager();
    ChunkPos pos = new ChunkPos(0, 0);
    assertFalse(wm.isChunkLoaded(pos));
    loadChunk(wm, 0, 0);
    assertTrue(wm.isChunkLoaded(pos));
  }

  @Test
  void getChunk() {
    WorldManagerImpl wm = createManager();
    ChunkPos pos = new ChunkPos(0, 0);
    assertNull(wm.getChunk(pos));
    loadChunk(wm, 0, 0);
    assertNotNull(wm.getChunk(pos));
    assertEquals(pos, wm.getChunk(pos).getPosition());
  }

  @Test
  void loadedChunkCount() {
    WorldManagerImpl wm = createManager();
    assertEquals(0, wm.loadedChunkCount());
    loadChunk(wm, 0, 0);
    assertEquals(1, wm.loadedChunkCount());
    loadChunk(wm, 1, 0);
    assertEquals(2, wm.loadedChunkCount());
  }

  @Test
  void getLoadedChunksReturnsSnapshot() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0);
    List<Chunk> snapshot = wm.getLoadedChunks();
    assertEquals(1, snapshot.size());
    loadChunk(wm, 1, 0);
    assertEquals(1, snapshot.size());
  }

  @Test
  void getLoadedChunksIsImmutable() {
    WorldManagerImpl wm = createManager();
    List<Chunk> snapshot = wm.getLoadedChunks();
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(null));
  }

  @Test
  void clearRemovesAllChunks() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0);
    loadChunk(wm, 1, 1);
    assertEquals(2, wm.loadedChunkCount());
    wm.clear();
    assertEquals(0, wm.loadedChunkCount());
  }

  @Test
  void clearPublishesWorldClearedEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    wm.clear();
    assertTrue(bus.contains(WorldClearedEvent.class));
  }

  @Test
  void clearInvokesListeners() {
    WorldManagerImpl wm = createManager();
    List<String> calls = new ArrayList<>();
    wm.addClearListener(() -> calls.add("cleared"));
    wm.clear();
    assertEquals(1, calls.size());
  }

  @Test
  void onChunkDataPublishesChunkLoadedEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    wm.onChunkData(chunkData(0, 0));
    assertTrue(bus.contains(ChunkLoadedEvent.class));
  }

  @Test
  void onChunkDataLoadsBlocks() {
    WorldManagerImpl wm = createManager();
    ChunkPos pos = new ChunkPos(0, 0);
    BlockState[] blocks = allAir();
    blocks[Chunk.index(5, 10, 7)] = new BlockState(42, 0);
    wm.onChunkData(new ChunkDataPacket(pos, blocks));
    assertEquals(new BlockState(42, 0), wm.getBlock(new BlockPos(5, 10, 7)));
  }

  @Test
  void onChunkDataReplacesExistingChunk() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0, new BlockState(1, 0), 0, 0, 0);
    loadChunk(wm, 0, 0, new BlockState(2, 0), 0, 0, 0);
    assertEquals(new BlockState(2, 0), wm.getBlock(new BlockPos(0, 0, 0)));
  }

  @Test
  void onBlockUpdateModifiesBlock() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0);
    BlockPos pos = new BlockPos(5, 10, 7);
    BlockState newState = new BlockState(10, 0);
    wm.onBlockUpdate(new BlockUpdatePacket(pos, newState));
    assertEquals(newState, wm.getBlock(pos));
  }

  @Test
  void onBlockUpdatePublishesEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    loadChunk(wm, 0, 0);
    wm.onBlockUpdate(new BlockUpdatePacket(new BlockPos(0, 0, 0), new BlockState(5, 0)));
    assertTrue(bus.contains(BlockUpdatedEvent.class));
  }

  @Test
  void onBlockUpdateForUnloadedChunkDoesNotCrash() {
    WorldManagerImpl wm = createManager();
    wm.onBlockUpdate(new BlockUpdatePacket(new BlockPos(0, 0, 0), new BlockState(5, 0)));
    assertEquals(BlockState.AIR, wm.getBlock(new BlockPos(0, 0, 0)));
  }

  @Test
  void onBlockUpdateUnchangedBlockDoesNotPublishEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    loadChunk(wm, 0, 0, BlockState.AIR, 0, 0, 0);
    wm.onBlockUpdate(new BlockUpdatePacket(new BlockPos(0, 0, 0), BlockState.AIR));
    assertFalse(bus.contains(BlockUpdatedEvent.class));
  }

  @Test
  void onMultiBlockUpdateModifiesBlocks() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0);
    BlockPos pos1 = new BlockPos(1, 1, 1);
    BlockPos pos2 = new BlockPos(2, 2, 2);
    BlockState state1 = new BlockState(10, 0);
    BlockState state2 = new BlockState(20, 0);
    wm.onMultiBlockUpdate(
        new MultiBlockUpdatePacket(
            new ChunkPos(0, 0), new BlockPos[] {pos1, pos2}, new BlockState[] {state1, state2}));
    assertEquals(state1, wm.getBlock(pos1));
    assertEquals(state2, wm.getBlock(pos2));
  }

  @Test
  void onMultiBlockUpdateForUnloadedChunkDoesNotCrash() {
    WorldManagerImpl wm = createManager();
    wm.onMultiBlockUpdate(
        new MultiBlockUpdatePacket(
            new ChunkPos(0, 0),
            new BlockPos[] {new BlockPos(0, 0, 0)},
            new BlockState[] {new BlockState(5, 0)}));
    assertEquals(BlockState.AIR, wm.getBlock(new BlockPos(0, 0, 0)));
  }

  @Test
  void onChunkUnloadRemovesChunk() {
    WorldManagerImpl wm = createManager();
    ChunkPos pos = new ChunkPos(0, 0);
    loadChunk(wm, 0, 0);
    assertTrue(wm.isChunkLoaded(pos));
    wm.onChunkUnload(new ChunkUnloadPacket(pos));
    assertFalse(wm.isChunkLoaded(pos));
  }

  @Test
  void onChunkUnloadPublishesEvent() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    loadChunk(wm, 0, 0);
    wm.onChunkUnload(new ChunkUnloadPacket(new ChunkPos(0, 0)));
    assertTrue(bus.contains(ChunkUnloadedEvent.class));
  }

  @Test
  void onChunkUnloadDuplicateIsNoOp() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    wm.onChunkUnload(new ChunkUnloadPacket(new ChunkPos(0, 0)));
    assertFalse(bus.contains(ChunkUnloadedEvent.class));
  }

  @Test
  void handlePacketDispatchesChunkData() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    wm.handlePacket(chunkData(0, 0));
    assertTrue(wm.isChunkLoaded(new ChunkPos(0, 0)));
    assertTrue(bus.contains(ChunkLoadedEvent.class));
  }

  @Test
  void handlePacketDispatchesBlockUpdate() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    loadChunk(wm, 0, 0);
    wm.handlePacket(new BlockUpdatePacket(new BlockPos(0, 0, 0), new BlockState(5, 0)));
    assertEquals(new BlockState(5, 0), wm.getBlock(new BlockPos(0, 0, 0)));
  }

  @Test
  void handlePacketDispatchesMultiBlockUpdate() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0);
    wm.handlePacket(
        new MultiBlockUpdatePacket(
            new ChunkPos(0, 0),
            new BlockPos[] {new BlockPos(0, 0, 0)},
            new BlockState[] {new BlockState(5, 0)}));
    assertEquals(new BlockState(5, 0), wm.getBlock(new BlockPos(0, 0, 0)));
  }

  @Test
  void handlePacketDispatchesChunkUnload() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0);
    wm.handlePacket(new ChunkUnloadPacket(new ChunkPos(0, 0)));
    assertFalse(wm.isChunkLoaded(new ChunkPos(0, 0)));
  }

  @Test
  void handlePacketIgnoresUnknownPackets() {
    WorldManagerImpl wm = createManager();
    wm.handlePacket(null);
    assertEquals(0, wm.loadedChunkCount());
  }

  @Test
  void concurrentLoadsDoNotCorrupt() throws Exception {
    WorldManagerImpl wm = createManager();
    int threadCount = 10;
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      int chunkX = i;
      threads[i] =
          Thread.startVirtualThread(
              () -> loadChunk(wm, chunkX, 0, new BlockState(chunkX, 0), 0, 0, 0));
    }
    for (Thread t : threads) {
      t.join();
    }
    assertEquals(threadCount, wm.loadedChunkCount());
    for (int i = 0; i < threadCount; i++) {
      assertEquals(new BlockState(i, 0), wm.getBlock(new BlockPos(i * 16, 0, 0)));
    }
  }

  @Test
  void concurrentReadsDuringWrite() throws Exception {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0, new BlockState(1, 0), 0, 0, 0);
    Thread reader =
        Thread.startVirtualThread(
            () -> {
              for (int i = 0; i < 1000; i++) {
                wm.getBlock(new BlockPos(0, 0, 0));
                wm.isChunkLoaded(new ChunkPos(0, 0));
              }
            });
    Thread writer =
        Thread.startVirtualThread(
            () -> {
              for (int i = 0; i < 100; i++) {
                wm.onBlockUpdate(
                    new BlockUpdatePacket(new BlockPos(0, 0, 0), new BlockState(i, 0)));
              }
            });
    reader.join();
    writer.join();
  }

  @Test
  void clearDuringRead() {
    WorldManagerImpl wm = createManager();
    loadChunk(wm, 0, 0);
    Thread thread =
        Thread.startVirtualThread(
            () -> {
              wm.clear();
              wm.getBlock(new BlockPos(0, 0, 0));
              wm.getLoadedChunks();
            });
    try {
      thread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    assertEquals(0, wm.loadedChunkCount());
  }

  @Test
  void blockUpdateEventContainsCorrectValues() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    loadChunk(wm, 0, 0);
    BlockPos pos = new BlockPos(5, 10, 7);
    BlockState oldState = BlockState.AIR;
    BlockState newState = new BlockState(42, 0);
    wm.onBlockUpdate(new BlockUpdatePacket(pos, newState));
    List<Event> events = bus.events();
    BlockUpdatedEvent event = (BlockUpdatedEvent) events.get(1);
    assertEquals(pos, event.getPos());
    assertEquals(oldState, event.getOldState());
    assertEquals(newState, event.getNewState());
  }

  @Test
  void chunkLoadedEventContainsCorrectPos() {
    RecordingEventBus bus = new RecordingEventBus();
    WorldManagerImpl wm = new WorldManagerImpl(BOT_ID, ACCOUNT_ID, bus, silentLogger());
    ChunkPos pos = new ChunkPos(3, 7);
    wm.onChunkData(chunkData(3, 7));
    ChunkLoadedEvent event = (ChunkLoadedEvent) bus.events().get(0);
    assertEquals(pos, event.getChunkPos());
  }

  // ---- helpers ----

  private static WorldManagerImpl createManager() {
    return new WorldManagerImpl(BOT_ID, ACCOUNT_ID, new RecordingEventBus(), silentLogger());
  }

  private static void loadChunk(WorldManagerImpl wm, int cx, int cz) {
    loadChunk(wm, cx, cz, BlockState.AIR, 0, 0, 0);
  }

  private static void loadChunk(
      WorldManagerImpl wm, int cx, int cz, BlockState block, int bx, int by, int bz) {
    ChunkPos pos = new ChunkPos(cx, cz);
    BlockState[] blocks = allAir();
    blocks[Chunk.index(bx, by, bz)] = block;
    wm.onChunkData(new ChunkDataPacket(pos, blocks));
  }

  private static ChunkDataPacket chunkData(int cx, int cz) {
    return new ChunkDataPacket(new ChunkPos(cx, cz), allAir());
  }

  private static BlockState[] allAir() {
    BlockState[] blocks = new BlockState[Chunk.BLOCK_COUNT];
    for (int i = 0; i < blocks.length; i++) {
      blocks[i] = BlockState.AIR;
    }
    return blocks;
  }

  private static Logger silentLogger() {
    return new Logger() {
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
        return true;
      }

      @Override
      public boolean isErrorEnabled() {
        return true;
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

  private static final class RecordingEventBus implements EventBus {
    private final List<Event> eventList = new CopyOnWriteArrayList<>();

    List<Event> events() {
      return List.copyOf(eventList);
    }

    boolean contains(Class<? extends Event> type) {
      return eventList.stream().anyMatch(type::isInstance);
    }

    @Override
    public <T extends Event> Subscription subscribe(Class<T> type, EventHandler<T> handler) {
      return () -> {};
    }

    @Override
    public <T extends Event> void unsubscribe(Class<T> type, EventHandler<T> handler) {}

    @Override
    public void publish(Event event) {
      eventList.add(event);
    }

    @Override
    public void publishAsync(Event event) {
      eventList.add(event);
    }
  }
}
