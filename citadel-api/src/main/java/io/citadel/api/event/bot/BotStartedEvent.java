package io.citadel.api.event.bot;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

/** Published when a Bot has successfully started and is in RUNNING state. */
public final class BotStartedEvent extends Event {

  private final UUID botId;
  private final String server;
  private final int port;

  public BotStartedEvent(UUID botId, String accountId, String server, int port) {
    super(null, accountId);
    this.botId = Objects.requireNonNull(botId, "botId");
    this.server = Objects.requireNonNull(server, "server");
    this.port = port;
  }

  public UUID getBotId() {
    return botId;
  }

  public String getServer() {
    return server;
  }

  public int getPort() {
    return port;
  }
}
