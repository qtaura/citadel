package io.citadel.api.event.network;

import io.citadel.api.event.Event;
import io.citadel.api.network.ProtocolState;
import java.util.Objects;

/** Published when a Minecraft connection changes protocol state. */
public final class ProtocolStateChangedEvent extends Event {

  private final String remoteAddress;
  private final int remotePort;
  private final ProtocolState previousState;
  private final ProtocolState newState;

  public ProtocolStateChangedEvent(
      String remoteAddress, int remotePort, ProtocolState previousState, ProtocolState newState) {
    this.remoteAddress = Objects.requireNonNull(remoteAddress, "remoteAddress");
    this.remotePort = remotePort;
    this.previousState = Objects.requireNonNull(previousState, "previousState");
    this.newState = Objects.requireNonNull(newState, "newState");
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public int getRemotePort() {
    return remotePort;
  }

  public ProtocolState getPreviousState() {
    return previousState;
  }

  public ProtocolState getNewState() {
    return newState;
  }
}
