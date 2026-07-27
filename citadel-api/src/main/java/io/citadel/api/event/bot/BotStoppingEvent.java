package io.citadel.api.event.bot;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

/** Published when a Bot begins its shutdown sequence. */
public final class BotStoppingEvent extends Event {

  private final UUID botId;

  public BotStoppingEvent(UUID botId, String accountId) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
  }

  public UUID getBotId() {
    return botId;
  }
}
