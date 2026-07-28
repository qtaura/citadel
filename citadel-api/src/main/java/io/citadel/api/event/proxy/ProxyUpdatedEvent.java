package io.citadel.api.event.proxy;

import io.citadel.api.event.Event;
import io.citadel.api.proxy.ProxyDefinition;
import java.util.Objects;

public final class ProxyUpdatedEvent extends Event {

  private final ProxyDefinition oldProxy;
  private final ProxyDefinition newProxy;

  public ProxyUpdatedEvent(ProxyDefinition oldProxy, ProxyDefinition newProxy) {
    super(null, newProxy.id());
    this.oldProxy = Objects.requireNonNull(oldProxy, "oldProxy");
    this.newProxy = Objects.requireNonNull(newProxy, "newProxy");
  }

  public ProxyDefinition getOldProxy() {
    return oldProxy;
  }

  public ProxyDefinition getNewProxy() {
    return newProxy;
  }
}
