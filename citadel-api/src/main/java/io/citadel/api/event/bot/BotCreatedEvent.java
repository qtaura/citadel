package io.citadel.api.event.bot;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

/** Published when a new Bot is created by the BotManager. */
public final class BotCreatedEvent extends Event {

  private final UUID botId;

  public BotCreatedEvent(UUID botId, String accountId) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
  }

  public UUID getBotId() {
    return botId;
  }
}
