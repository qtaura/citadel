package io.citadel.api.event.network;

import io.citadel.api.event.Event;
import io.citadel.api.network.ServerListPingStatus;

public final class StatusReceivedEvent extends Event {

  private final ServerListPingStatus status;
  private final String remoteAddress;
  private final int remotePort;

  public StatusReceivedEvent(ServerListPingStatus status, String remoteAddress, int remotePort) {
    this.status = status;
    this.remoteAddress = remoteAddress;
    this.remotePort = remotePort;
  }

  public ServerListPingStatus getStatus() {
    return status;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public int getRemotePort() {
    return remotePort;
  }
}
