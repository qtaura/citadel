package io.citadel.core.world;

import io.citadel.api.event.EventBus;
import io.citadel.api.event.world.BlockUpdatedEvent;
import io.citadel.api.event.world.ChunkLoadedEvent;
import io.citadel.api.event.world.ChunkUnloadedEvent;
import io.citadel.api.event.world.WorldClearedEvent;
import io.citadel.api.event.world.WorldLoadedEvent;
import io.citadel.api.service.Logger;
import io.citadel.api.world.BlockPos;
import io.citadel.api.world.BlockState;
import io.citadel.api.world.Chunk;
import io.citadel.api.world.ChunkPos;
import io.citadel.api.world.WorldManager;
import io.citadel.core.net.Packet;
import io.citadel.core.net.protocol.play.BlockUpdatePacket;
import io.citadel.core.net.protocol.play.ChunkDataPacket;
import io.citadel.core.net.protocol.play.ChunkUnloadPacket;
import io.citadel.core.net.protocol.play.MultiBlockUpdatePacket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WorldManagerImpl implements WorldManager {

  private final ConcurrentMap<ChunkPos, Chunk> chunks;
  private final EventBus eventBus;
  private final Logger logger;
  private final UUID botId;
  private final String accountId;
  private final List<Runnable> clearListeners;

  public WorldManagerImpl(UUID botId, String accountId, EventBus eventBus, Logger logger) {
    this.chunks = new ConcurrentHashMap<>();
    this.eventBus = eventBus;
    this.logger = logger;
    this.botId = botId;
    this.accountId = accountId;
    this.clearListeners = new CopyOnWriteArrayList<>();
    eventBus.subscribe(WorldLoadedEvent.class, e -> clear());
  }

  @Override
  public BlockState getBlock(BlockPos position) {
    ChunkPos cp = position.chunkPos();
    Chunk chunk = chunks.get(cp);
    if (chunk == null) {
      return BlockState.AIR;
    }
    return chunk.getBlock(position.x() & 0xF, position.y(), position.z() & 0xF);
  }

  @Override
  public Chunk getChunk(ChunkPos position) {
    return chunks.get(position);
  }

  @Override
  public boolean isChunkLoaded(ChunkPos position) {
    return chunks.containsKey(position);
  }

  @Override
  public List<Chunk> getLoadedChunks() {
    return List.copyOf(chunks.values());
  }

  @Override
  public int loadedChunkCount() {
    return chunks.size();
  }

  @Override
  public void clear() {
    chunks.clear();
    eventBus.publishAsync(new WorldClearedEvent(botId, accountId));
    for (Runnable listener : clearListeners) {
      listener.run();
    }
  }

  public void onChunkData(ChunkDataPacket packet) {
    ChunkPos pos = packet.getChunkPos();
    Chunk chunk = new Chunk(pos, packet.getBlocks());
    chunks.put(pos, chunk);
    eventBus.publishAsync(new ChunkLoadedEvent(botId, accountId, pos));
  }

  public void onBlockUpdate(BlockUpdatePacket packet) {
    BlockPos pos = packet.getPos();
    ChunkPos cp = pos.chunkPos();
    Chunk oldChunk = chunks.get(cp);
    if (oldChunk == null) {
      logger.warn("Block update for unloaded chunk {} (bot {})", cp, botId);
      return;
    }
    int lx = pos.x() & 0xF;
    int ly = pos.y();
    int lz = pos.z() & 0xF;
    BlockState oldState = oldChunk.getBlock(lx, ly, lz);
    if (oldState.equals(packet.getState())) {
      return;
    }
    BlockState[] newBlocks = oldChunk.getBlocks();
    newBlocks[Chunk.index(lx, ly, lz)] = packet.getState();
    Chunk newChunk = new Chunk(cp, newBlocks);
    chunks.put(cp, newChunk);
    eventBus.publishAsync(
        new BlockUpdatedEvent(botId, accountId, pos, oldState, packet.getState()));
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  public void onMultiBlockUpdate(MultiBlockUpdatePacket packet) {
    ChunkPos cp = packet.getChunkPos();
    Chunk oldChunk = chunks.get(cp);
    if (oldChunk == null) {
      logger.warn("Multi-block update for unloaded chunk {} (bot {})", cp, botId);
      return;
    }
    BlockState[] newBlocks = oldChunk.getBlocks();
    BlockPos[] positions = packet.getPositions();
    BlockState[] states = packet.getStates();
    for (int i = 0; i < positions.length; i++) {
      BlockPos pos = positions[i];
      BlockState state = states[i];
      int lx = pos.x() & 0xF;
      int ly = pos.y();
      int lz = pos.z() & 0xF;
      int idx = Chunk.index(lx, ly, lz);
      BlockState oldState = newBlocks[idx];
      if (!oldState.equals(state)) {
        newBlocks[idx] = state;
        eventBus.publishAsync(new BlockUpdatedEvent(botId, accountId, pos, oldState, state));
      }
    }
    Chunk newChunk = new Chunk(cp, newBlocks);
    chunks.put(cp, newChunk);
  }

  public void onChunkUnload(ChunkUnloadPacket packet) {
    ChunkPos pos = packet.getChunkPos();
    Chunk removed = chunks.remove(pos);
    if (removed != null) {
      eventBus.publishAsync(new ChunkUnloadedEvent(botId, accountId, pos));
    }
  }

  public void handlePacket(Packet packet) {
    if (packet instanceof ChunkDataPacket p) {
      onChunkData(p);
    } else if (packet instanceof BlockUpdatePacket p) {
      onBlockUpdate(p);
    } else if (packet instanceof MultiBlockUpdatePacket p) {
      onMultiBlockUpdate(p);
    } else if (packet instanceof ChunkUnloadPacket p) {
      onChunkUnload(p);
    }
  }

  public void addClearListener(Runnable listener) {
    clearListeners.add(listener);
  }

  int internalChunkCount() {
    return chunks.size();
  }
}
