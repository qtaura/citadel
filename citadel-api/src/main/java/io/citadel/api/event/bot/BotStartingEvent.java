package io.citadel.api.event.bot;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

/** Published when a Bot begins its startup sequence. */
public final class BotStartingEvent extends Event {

  private final UUID botId;

  public BotStartingEvent(UUID botId, String accountId) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
  }

  public UUID getBotId() {
    return botId;
  }
}
