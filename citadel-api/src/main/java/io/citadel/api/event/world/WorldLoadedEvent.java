package io.citadel.api.event.world;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

public final class WorldLoadedEvent extends Event {

  private final UUID botId;

  public WorldLoadedEvent(UUID botId, String accountId) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
  }

  public UUID getBotId() {
    return botId;
  }
}
