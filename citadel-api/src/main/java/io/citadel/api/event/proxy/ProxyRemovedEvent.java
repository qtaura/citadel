package io.citadel.api.event.proxy;

import io.citadel.api.event.Event;
import io.citadel.api.proxy.ProxyDefinition;
import java.util.Objects;

public final class ProxyRemovedEvent extends Event {

  private final ProxyDefinition proxy;

  public ProxyRemovedEvent(ProxyDefinition proxy) {
    super(null, proxy.id());
    this.proxy = Objects.requireNonNull(proxy, "proxy");
  }

  public ProxyDefinition getProxy() {
    return proxy;
  }
}
