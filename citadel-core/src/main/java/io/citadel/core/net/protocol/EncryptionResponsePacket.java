package io.citadel.core.net.protocol;

import io.citadel.api.network.ProtocolState;
import io.citadel.core.net.Packet;
import io.citadel.core.net.VarInt;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class EncryptionResponsePacket implements Packet {

  private byte[] sharedSecret;
  private byte[] verifyToken;

  public EncryptionResponsePacket() {}

  public EncryptionResponsePacket(byte[] sharedSecret, byte[] verifyToken) {
    this.sharedSecret = sharedSecret.clone();
    this.verifyToken = verifyToken.clone();
  }

  public byte[] getSharedSecret() {
    return sharedSecret.clone();
  }

  public byte[] getVerifyToken() {
    return verifyToken.clone();
  }

  @Override
  public int getPacketId(ProtocolState state) {
    return 0x01;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    VarInt.write(sharedSecret.length, out);
    out.write(sharedSecret);
    VarInt.write(verifyToken.length, out);
    out.write(verifyToken);
  }

  @Override
  public void read(DataInput in) throws IOException {
    int secretLen = VarInt.read(in);
    this.sharedSecret = new byte[secretLen];
    in.readFully(sharedSecret);
    int tokenLen = VarInt.read(in);
    this.verifyToken = new byte[tokenLen];
    in.readFully(verifyToken);
  }
}
