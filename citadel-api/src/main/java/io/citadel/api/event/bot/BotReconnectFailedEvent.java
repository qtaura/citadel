package io.citadel.api.event.bot;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

public final class BotReconnectFailedEvent extends Event {

  private final UUID botId;
  private final int attempt;
  private final String reason;

  public BotReconnectFailedEvent(UUID botId, String accountId, int attempt, String reason) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
    this.attempt = attempt;
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public UUID getBotId() {
    return botId;
  }

  public int getAttempt() {
    return attempt;
  }

  public String getReason() {
    return reason;
  }
}
