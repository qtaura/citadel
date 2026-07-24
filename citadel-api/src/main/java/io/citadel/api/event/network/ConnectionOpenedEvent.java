package io.citadel.api.event.network;

import io.citadel.api.event.Event;
import io.citadel.api.network.ConnectionState;

public final class ConnectionOpenedEvent extends Event {

  private final String remoteAddress;
  private final int remotePort;
  private final ConnectionState previousState;

  public ConnectionOpenedEvent(
      String remoteAddress, int remotePort, ConnectionState previousState) {
    this.remoteAddress = remoteAddress;
    this.remotePort = remotePort;
    this.previousState = previousState;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public int getRemotePort() {
    return remotePort;
  }

  public ConnectionState getPreviousState() {
    return previousState;
  }
}
