package io.citadel.core.net;

public final class ConnectivityTest {
  public static void main(String[] args) throws Exception {
    String host = args.length > 0 ? args[0] : "8b8t.org";
    int port = args.length > 1 ? Integer.parseInt(args[1]) : 25565;
    int timeout = args.length > 2 ? Integer.parseInt(args[2]) : 15000;
    int protocol = args.length > 3 ? Integer.parseInt(args[3]) : 767;
    
    System.out.println("Testing " + host + ":" + port + " (timeout: " + timeout + "ms, protocol: " + protocol + ")...");
    java.net.Socket socket = new java.net.Socket();
    try {
      long t1 = System.currentTimeMillis();
      socket.connect(new java.net.InetSocketAddress(host, port), timeout);
      long t2 = System.currentTimeMillis();
      System.out.println("Connected in " + (t2 - t1) + "ms");
      socket.setSoTimeout(timeout);
      java.io.DataInputStream in = new java.io.DataInputStream(socket.getInputStream());
      java.io.DataOutputStream out = new java.io.DataOutputStream(socket.getOutputStream());
      
      // Send handshake 
      sendFrame(out, 0, buf -> {
        VarInt.write(protocol, buf);
        MinecraftStrings.write(host, buf);
        buf.writeShort(port);
        VarInt.write(2, buf); // nextState = LOGIN
      });
      System.out.println("Sent Handshake");
      
      // Send LoginStart
      String username = args.length > 4 ? args[4] : "TestBot";
      java.util.UUID offlineUuid = java.util.UUID.nameUUIDFromBytes(
        ("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      sendFrame(out, 0, buf -> {
        MinecraftStrings.write(username, buf);
        buf.writeLong(offlineUuid.getMostSignificantBits());
        buf.writeLong(offlineUuid.getLeastSignificantBits());
      });
      System.out.println("Sent LoginStart for " + username);
      System.out.println("Waiting for server response...");
      
      long t3 = System.currentTimeMillis();
      int packetLen = VarInt.read(in);
      long t4 = System.currentTimeMillis();
      System.out.println("Got response (" + packetLen + " bytes) in " + (t4 - t3) + "ms");
      
      byte[] packet = new byte[packetLen];
      in.readFully(packet);
      int packetId = packet.length > 0 ? (packet[0] & 0xFF) : -1;
      System.out.println("Packet ID: 0x" + Integer.toHexString(packetId));
      System.out.println("Data (" + packet.length + " bytes): " + bytesToHex(packet, Math.min(40, packet.length)));
      
      socket.close();
      System.out.println("SUCCESS!");
    } catch (java.net.SocketTimeoutException e) {
      System.out.println("TIMEOUT after " + timeout + "ms");
    } catch (Exception e) {
      System.out.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
    } finally {
      try { socket.close(); } catch (Exception e) {}
    }
  }
  
  @FunctionalInterface
  interface BodyWriter { void write(java.io.DataOutputStream buf) throws Exception; }
  
  static void sendFrame(java.io.DataOutputStream out, int packetId, BodyWriter writer) throws Exception {
    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
    java.io.DataOutputStream body = new java.io.DataOutputStream(buf);
    VarInt.write(packetId, body);
    writer.write(body);
    byte[] packetBytes = buf.toByteArray();
    java.io.ByteArrayOutputStream frame = new java.io.ByteArrayOutputStream();
    java.io.DataOutputStream frameOut = new java.io.DataOutputStream(frame);
    VarInt.write(packetBytes.length, frameOut);
    frameOut.write(packetBytes);
    out.write(frame.toByteArray());
    out.flush();
  }
  
  static String bytesToHex(byte[] bytes, int len) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < Math.min(len, bytes.length); i++) {
      sb.append(String.format("%02X ", bytes[i]));
    }
    return sb.toString();
  }
}
