package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.MinecraftStrings;
import io.citadel.core.net.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

public final class LoginStartPacket implements Packet {

  private String username;
  private UUID profileId;

  public LoginStartPacket() {}

  public LoginStartPacket(String username, UUID profileId) {
    this.username = username;
    this.profileId = profileId;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x00;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    MinecraftStrings.write(username, out);
    out.writeLong(profileId.getMostSignificantBits());
    out.writeLong(profileId.getLeastSignificantBits());
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.username = MinecraftStrings.read(in);
    this.profileId = new UUID(in.readLong(), in.readLong());
  }

  public String getUsername() {
    return username;
  }

  public UUID getProfileId() {
    return profileId;
  }
}
