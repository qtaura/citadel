package io.citadel.api.event.network;

import io.citadel.api.event.Event;
import io.citadel.api.network.ConnectionState;

public final class ConnectionClosedEvent extends Event {

  private final String remoteAddress;
  private final int remotePort;
  private final ConnectionState previousState;
  private final String reason;

  public ConnectionClosedEvent(
      String remoteAddress, int remotePort, ConnectionState previousState, String reason) {
    this.remoteAddress = remoteAddress;
    this.remotePort = remotePort;
    this.previousState = previousState;
    this.reason = reason;
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

  public String getReason() {
    return reason;
  }
}
