package io.citadel.api.event.bot;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

public final class BotReconnectStartedEvent extends Event {

  private final UUID botId;
  private final int attempt;
  private final int maxAttempts;

  public BotReconnectStartedEvent(UUID botId, String accountId, int attempt, int maxAttempts) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
    this.attempt = attempt;
    this.maxAttempts = maxAttempts;
  }

  public UUID getBotId() {
    return botId;
  }

  public int getAttempt() {
    return attempt;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }
}
