package io.citadel.core.net;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PacketRegistry {

  private final Map<Integer, PacketCodec> codecs;

  private PacketRegistry(Map<Integer, PacketCodec> codecs) {
    this.codecs = codecs;
  }

  public PacketCodec getCodec(int packetId) {
    PacketCodec codec = codecs.get(packetId);
    if (codec == null) {
      return null;
    }
    return codec;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final Map<Integer, PacketCodec> codecs = new HashMap<>();

    Builder() {}

    public Builder register(int packetId, PacketCodec codec) {
      codecs.put(packetId, Objects.requireNonNull(codec, "codec"));
      return this;
    }

    public PacketRegistry build() {
      return new PacketRegistry(Collections.unmodifiableMap(new HashMap<>(codecs)));
    }
  }
}
