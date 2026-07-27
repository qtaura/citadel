package io.citadel.api.event.auth;

import io.citadel.api.event.Event;
import java.util.Objects;

/** Published when authentication or Minecraft login fails. */
public final class LoginFailedEvent extends Event {

  private final String username;
  private final String reason;
  private final String remoteAddress;
  private final int remotePort;

  public LoginFailedEvent(
      String accountId, String username, String reason, String remoteAddress, int remotePort) {
    super(null, accountId);
    this.username = Objects.requireNonNull(username, "username");
    this.reason = Objects.requireNonNull(reason, "reason");
    this.remoteAddress = Objects.requireNonNull(remoteAddress, "remoteAddress");
    this.remotePort = remotePort;
  }

  public String getUsername() {
    return username;
  }

  public String getReason() {
    return reason;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public int getRemotePort() {
    return remotePort;
  }
}
