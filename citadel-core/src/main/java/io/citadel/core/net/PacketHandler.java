package io.citadel.core.net;

@FunctionalInterface
public interface PacketHandler {
  void handle(Packet packet);
}
