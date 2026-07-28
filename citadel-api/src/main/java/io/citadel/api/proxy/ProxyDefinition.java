package io.citadel.api.proxy;

import java.util.Objects;

public record ProxyDefinition(String id, ProxyType type, String host, int port) {

  public ProxyDefinition {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    if (type != ProxyType.DIRECT) {
      Objects.requireNonNull(host, "host");
      if (host.isBlank()) {
        throw new IllegalArgumentException("host must not be blank for type " + type);
      }
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("port must be between 1 and 65535");
      }
    }
  }
}
