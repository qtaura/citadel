package io.citadel.api.event.bot;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

/** Published when a Bot encounters a failure during its lifecycle. */
public final class BotFailedEvent extends Event {

  private final UUID botId;
  private final String reason;

  public BotFailedEvent(UUID botId, String accountId, String reason) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public UUID getBotId() {
    return botId;
  }

  public String getReason() {
    return reason;
  }
}
