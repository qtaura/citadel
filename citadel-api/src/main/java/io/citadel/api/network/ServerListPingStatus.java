package io.citadel.api.network;

import java.util.Objects;

public final class ServerListPingStatus {

  private final int protocolVersion;
  private final String serverVersion;
  private final String motd;
  private final int maxPlayers;
  private final int onlinePlayers;

  public ServerListPingStatus(
      int protocolVersion, String serverVersion, String motd, int maxPlayers, int onlinePlayers) {
    this.protocolVersion = protocolVersion;
    this.serverVersion = Objects.requireNonNull(serverVersion, "serverVersion");
    this.motd = Objects.requireNonNull(motd, "motd");
    this.maxPlayers = maxPlayers;
    this.onlinePlayers = onlinePlayers;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public String getVersion() {
    return serverVersion;
  }

  public String getMotd() {
    return motd;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public int getOnlinePlayers() {
    return onlinePlayers;
  }
}
