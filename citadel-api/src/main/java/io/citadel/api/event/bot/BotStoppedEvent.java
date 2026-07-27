package io.citadel.api.event.bot;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

/** Published when a Bot has fully stopped. */
public final class BotStoppedEvent extends Event {

  private final UUID botId;

  public BotStoppedEvent(UUID botId, String accountId) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
  }

  public UUID getBotId() {
    return botId;
  }
}
