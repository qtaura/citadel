package io.citadel.api.event.auth;

import io.citadel.api.event.Event;
import java.util.Objects;
import java.util.UUID;

/** Published when a Minecraft login succeeds and yields a session profile. */
public final class LoginSucceededEvent extends Event {

  private final String username;
  private final UUID profileId;
  private final String remoteAddress;
  private final int remotePort;

  public LoginSucceededEvent(
      String accountId, String username, UUID profileId, String remoteAddress, int remotePort) {
    super(null, accountId);
    this.username = Objects.requireNonNull(username, "username");
    this.profileId = Objects.requireNonNull(profileId, "profileId");
    this.remoteAddress = Objects.requireNonNull(remoteAddress, "remoteAddress");
    this.remotePort = remotePort;
  }

  public String getUsername() {
    return username;
  }

  public UUID getProfileId() {
    return profileId;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public int getRemotePort() {
    return remotePort;
  }
}
