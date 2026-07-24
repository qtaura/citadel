package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.core.net.protocol.StatusProtocolCodecs;
import org.junit.jupiter.api.Test;

class PacketRegistryTest {

  @Test
  void registerAndLookup() {
    PacketRegistry registry =
        PacketRegistry.builder().register(0x00, StatusProtocolCodecs.statusResponse()).build();
    assertNotNull(registry.getCodec(0x00));
  }

  @Test
  void lookupMissingPacketReturnsNull() {
    PacketRegistry registry = PacketRegistry.builder().build();
    assertNull(registry.getCodec(0xFF));
  }

  @Test
  void registryIsImmutableAfterBuild() {
    PacketRegistry.Builder builder = PacketRegistry.builder();
    builder.register(0x00, StatusProtocolCodecs.statusResponse());
    PacketRegistry registry = builder.build();
    assertNotNull(registry.getCodec(0x00));
  }

  @Test
  void multipleRegistrations() {
    PacketRegistry registry =
        PacketRegistry.builder()
            .register(0x00, StatusProtocolCodecs.statusResponse())
            .register(0x01, StatusProtocolCodecs.pingResponse())
            .build();
    assertNotNull(registry.getCodec(0x00));
    assertNotNull(registry.getCodec(0x01));
  }

  @Test
  void overwriteRegistration() {
    PacketRegistry registry =
        PacketRegistry.builder()
            .register(0x00, StatusProtocolCodecs.statusResponse())
            .register(0x00, StatusProtocolCodecs.pingResponse())
            .build();
    assertNotNull(registry.getCodec(0x00));
  }
}
