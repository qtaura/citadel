package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.MinecraftStrings;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

public final class LoginSuccessPacket implements Packet {

  private UUID profileId;
  private String username;
  private boolean strictErrorHandling;

  public LoginSuccessPacket() {}

  public LoginSuccessPacket(UUID profileId, String username) {
    this(profileId, username, false);
  }

  public LoginSuccessPacket(UUID profileId, String username, boolean strictErrorHandling) {
    this.profileId = profileId;
    this.username = username;
    this.strictErrorHandling = strictErrorHandling;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x02;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeLong(profileId.getMostSignificantBits());
    out.writeLong(profileId.getLeastSignificantBits());
    MinecraftStrings.write(username, out);
    VarInt.write(0, out);
    out.writeBoolean(strictErrorHandling);
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.profileId = new UUID(in.readLong(), in.readLong());
    this.username = MinecraftStrings.read(in);
    int propertyCount = VarInt.read(in);
    for (int i = 0; i < propertyCount; i++) {
      MinecraftStrings.read(in);
      MinecraftStrings.read(in);
      if (in.readBoolean()) {
        MinecraftStrings.read(in);
      }
    }
    this.strictErrorHandling = in.readBoolean();
  }

  public UUID getProfileId() {
    return profileId;
  }

  public String getUsername() {
    return username;
  }

  public boolean isStrictErrorHandling() {
    return strictErrorHandling;
  }
}
