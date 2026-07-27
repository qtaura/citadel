package io.citadel.api.event.auth;

import io.citadel.api.event.Event;
import java.util.Objects;

/** Published when Citadel begins the Minecraft login protocol for an account. */
public final class LoginStartedEvent extends Event {

  private final String username;
  private final String remoteAddress;
  private final int remotePort;

  public LoginStartedEvent(
      String accountId, String username, String remoteAddress, int remotePort) {
    super(null, accountId);
    this.username = Objects.requireNonNull(username, "username");
    this.remoteAddress = Objects.requireNonNull(remoteAddress, "remoteAddress");
    this.remotePort = remotePort;
  }

  public String getUsername() {
    return username;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public int getRemotePort() {
    return remotePort;
  }
}
