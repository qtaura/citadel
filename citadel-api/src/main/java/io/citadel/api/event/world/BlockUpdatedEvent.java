package io.citadel.api.event.world;

import io.citadel.api.event.Event;
import io.citadel.api.world.BlockPos;
import io.citadel.api.world.BlockState;
import java.util.Objects;
import java.util.UUID;

public final class BlockUpdatedEvent extends Event {

  private final UUID botId;
  private final BlockPos pos;
  private final BlockState oldState;
  private final BlockState newState;

  public BlockUpdatedEvent(
      UUID botId, String accountId, BlockPos pos, BlockState oldState, BlockState newState) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
    this.pos = Objects.requireNonNull(pos, "pos");
    this.oldState = Objects.requireNonNull(oldState, "oldState");
    this.newState = Objects.requireNonNull(newState, "newState");
  }

  public UUID getBotId() {
    return botId;
  }

  public BlockPos getPos() {
    return pos;
  }

  public BlockState getOldState() {
    return oldState;
  }

  public BlockState getNewState() {
    return newState;
  }
}
