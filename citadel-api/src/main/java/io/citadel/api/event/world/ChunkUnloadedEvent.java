package io.citadel.api.event.world;

import io.citadel.api.event.Event;
import io.citadel.api.world.ChunkPos;
import java.util.Objects;
import java.util.UUID;

public final class ChunkUnloadedEvent extends Event {

  private final UUID botId;
  private final ChunkPos chunkPos;

  public ChunkUnloadedEvent(UUID botId, String accountId, ChunkPos chunkPos) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
    this.chunkPos = Objects.requireNonNull(chunkPos, "chunkPos");
  }

  public UUID getBotId() {
    return botId;
  }

  public ChunkPos getChunkPos() {
    return chunkPos;
  }
}
