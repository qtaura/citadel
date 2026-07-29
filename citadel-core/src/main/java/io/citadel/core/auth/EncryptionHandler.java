package io.citadel.core.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class EncryptionHandler {

  private final SecretKey sharedSecret;
  private final byte[] serverPublicKeyBytes;
  private final byte[] encryptedSharedSecret;
  private final byte[] encryptedVerifyToken;

  public EncryptionHandler(byte[] serverPublicKeyBytes, byte[] verifyToken) {
    try {
      this.serverPublicKeyBytes = serverPublicKeyBytes.clone();
      this.sharedSecret = generateSharedSecret();
      java.security.PublicKey serverKey = readPublicKey(serverPublicKeyBytes);
      Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      rsa.init(Cipher.ENCRYPT_MODE, serverKey);
      this.encryptedSharedSecret = rsa.doFinal(sharedSecret.getEncoded());
      this.encryptedVerifyToken = rsa.doFinal(verifyToken);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set up encryption", e);
    }
  }

  public SecretKey getSharedSecret() {
    return sharedSecret;
  }

  public byte[] getEncryptedSharedSecret() {
    return encryptedSharedSecret.clone();
  }

  public byte[] getEncryptedVerifyToken() {
    return encryptedVerifyToken.clone();
  }

  public String computeServerId() {
    try {
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      sha1.update(sharedSecret.getEncoded());
      sha1.update(serverPublicKeyBytes);
      byte[] hash = sha1.digest();
      String hex = new java.math.BigInteger(hash).toString(16);
      if (hex.startsWith("-")) {
        return "-" + hex.substring(1);
      }
      return hex;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-1 not available", e);
    }
  }

  public static Cipher createEncryptionCipher(SecretKey key, int mode) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
      IvParameterSpec iv = new IvParameterSpec(key.getEncoded());
      cipher.init(mode, new SecretKeySpec(key.getEncoded(), "AES"), iv);
      return cipher;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create AES cipher", e);
    }
  }

  private static SecretKey generateSharedSecret() {
    try {
      KeyGenerator kg = KeyGenerator.getInstance("AES");
      kg.init(128);
      return kg.generateKey();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("AES not available", e);
    }
  }

  private static java.security.PublicKey readPublicKey(byte[] encodedKey) {
    try {
      java.security.KeyFactory factory = java.security.KeyFactory.getInstance("RSA");
      return factory.generatePublic(new java.security.spec.X509EncodedKeySpec(encodedKey));
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse server public key", e);
    }
  }
}
