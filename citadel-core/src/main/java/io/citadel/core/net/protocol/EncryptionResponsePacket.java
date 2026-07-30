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
  private boolean hasSignature;
  private long salt;
  private byte[] signature;

  public EncryptionResponsePacket() {}

  public EncryptionResponsePacket(byte[] sharedSecret, byte[] verifyToken) {
    this(sharedSecret, verifyToken, 0L, new byte[0]);
  }

  public EncryptionResponsePacket(byte[] sharedSecret, byte[] verifyToken, long salt, byte[] signature) {
    this.sharedSecret = sharedSecret.clone();
    this.verifyToken = verifyToken.clone();
    this.hasSignature = signature.length > 0;
    this.salt = salt;
    this.signature = signature.clone();
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
    if (hasSignature) {
      out.writeLong(salt);
      VarInt.write(signature.length, out);
      out.write(signature);
    }
  }

  @Override
  public void read(DataInput in) throws IOException {
    int secretLen = VarInt.read(in);
    this.sharedSecret = new byte[secretLen];
    in.readFully(sharedSecret);
    int tokenLen = VarInt.read(in);
    this.verifyToken = new byte[tokenLen];
    in.readFully(verifyToken);
    this.hasSignature = false;
    this.signature = new byte[0];
  }
}
