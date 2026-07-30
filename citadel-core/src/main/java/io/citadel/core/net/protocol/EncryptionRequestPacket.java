package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.MinecraftStrings;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class EncryptionRequestPacket implements Packet {

  private String serverId;
  private byte[] publicKey;
  private byte[] verifyToken;
  private boolean shouldAuthenticate;

  public EncryptionRequestPacket() {}

  public EncryptionRequestPacket(String serverId, byte[] publicKey, byte[] verifyToken) {
    this(serverId, publicKey, verifyToken, false);
  }

  public EncryptionRequestPacket(String serverId, byte[] publicKey, byte[] verifyToken, boolean shouldAuthenticate) {
    this.serverId = serverId;
    this.publicKey = publicKey.clone();
    this.verifyToken = verifyToken.clone();
    this.shouldAuthenticate = shouldAuthenticate;
  }

  public String getServerId() {
    return serverId;
  }

  public byte[] getPublicKey() {
    return publicKey.clone();
  }

  public byte[] getVerifyToken() {
    return verifyToken.clone();
  }

  public boolean shouldAuthenticate() {
    return shouldAuthenticate;
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x01;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void read(DataInput in) throws IOException {
    this.serverId = MinecraftStrings.read(in);
    int keyLen = VarInt.read(in);
    this.publicKey = new byte[keyLen];
    in.readFully(publicKey);
    int tokenLen = VarInt.read(in);
    this.verifyToken = new byte[tokenLen];
    in.readFully(verifyToken);
    try {
      this.shouldAuthenticate = in.readBoolean();
    } catch (Exception e) {
      this.shouldAuthenticate = false;
    }
  }
}
