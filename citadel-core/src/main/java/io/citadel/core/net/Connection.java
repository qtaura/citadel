package io.citadel.core.net;

import io.citadel.api.event.EventBus;
import io.citadel.api.event.network.ConnectionClosedEvent;
import io.citadel.api.event.network.ConnectionOpenedEvent;
import io.citadel.api.event.network.PacketReceivedEvent;
import io.citadel.api.event.network.PacketSentEvent;
import io.citadel.api.event.network.ProtocolStateChangedEvent;
import io.citadel.api.network.ConnectionState;
import io.citadel.api.network.ProtocolState;
import io.citadel.api.proxy.ProxyDefinition;
import io.citadel.api.proxy.ProxyType;
import io.citadel.api.service.Logger;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Connection implements io.citadel.api.network.Connection, Closeable {

  private final AtomicReference<ConnectionState> state;
  private final AtomicReference<ProtocolState> protocolState;
  private final String host;
  private final int port;
  private final int connectTimeout;
  private int readTimeout;
  private final EventBus eventBus;
  private final Logger logger;
  private final ProxyDefinition proxy;

  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;

  public Connection(
      String host,
      int port,
      int connectTimeout,
      int readTimeout,
      EventBus eventBus,
      Logger logger) {
    this(host, port, connectTimeout, readTimeout, eventBus, logger, null);
  }

  public Connection(
      String host,
      int port,
      int connectTimeout,
      int readTimeout,
      EventBus eventBus,
      Logger logger,
      ProxyDefinition proxy) {
    this.state = new AtomicReference<>(ConnectionState.CREATED);
    this.protocolState = new AtomicReference<>(ProtocolState.HANDSHAKE);
    this.host = host;
    this.port = port;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.eventBus = eventBus;
    this.logger = Objects.requireNonNull(logger, "logger");
    this.proxy = proxy;
  }

  @Override
  public void connect() throws IOException {
    if (!state.compareAndSet(ConnectionState.CREATED, ConnectionState.CONNECTING)) {
      throw new IllegalStateException("Cannot connect from state: " + state.get());
    }
    ConnectionState prev = ConnectionState.CREATED;
    String proxySuffix = proxy != null ? " via " + proxy.type() + ":" + proxy.id() : "";
    logger.info("Connecting to {}:{}{} ...", host, port, proxySuffix);
    Socket s = null;
    try {
      s = openSocket();
      this.socket = s;
      this.in = new DataInputStream(s.getInputStream());
      this.out = new DataOutputStream(s.getOutputStream());
      state.set(ConnectionState.CONNECTED);
      logger.info("Connected to {}:{}{}", host, port, proxySuffix);
      eventBus.publishAsync(new ConnectionOpenedEvent(host, port, prev));
    } catch (IOException e) {
      closeQuietly(s);
      state.set(ConnectionState.CLOSED);
      logger.error("Failed to connect to {}:{}: {}", host, port, e.getMessage());
      throw e;
    }
  }

  private Socket openSocket() throws IOException {
    if (proxy != null) {
      if (proxy.type() == ProxyType.HTTP) {
        return connectViaHttpProxy();
      }
      if (proxy.type() == ProxyType.SOCKS5) {
        return connectViaSocks5();
      }
    }
    Socket s = new Socket();
    s.connect(new InetSocketAddress(host, port), connectTimeout);
    s.setSoTimeout(readTimeout);
    return s;
  }

  private Socket connectViaSocks5() throws IOException {
    java.net.Proxy jProxy =
        new java.net.Proxy(
            java.net.Proxy.Type.SOCKS, new InetSocketAddress(proxy.host(), proxy.port()));
    Socket s = new Socket(jProxy);
    s.connect(new InetSocketAddress(host, port), connectTimeout);
    s.setSoTimeout(readTimeout);
    return s;
  }

  private Socket connectViaHttpProxy() throws IOException {
    Socket s = new Socket();
    s.connect(new InetSocketAddress(proxy.host(), proxy.port()), connectTimeout);
    s.setSoTimeout(readTimeout);
    OutputStream rawOut = s.getOutputStream();
    String connectRequest =
        "CONNECT " + host + ":" + port + " HTTP/1.1\r\nHost: " + host + ":" + port + "\r\n\r\n";
    rawOut.write(connectRequest.getBytes(StandardCharsets.UTF_8));
    rawOut.flush();
    InputStream rawIn = s.getInputStream();
    String statusLine = readHttpLine(rawIn);
    if (statusLine == null || statusLine.isEmpty() || !statusLine.contains("200")) {
      s.close();
      throw new IOException(
          "HTTP CONNECT failed: "
              + (statusLine != null && !statusLine.isEmpty() ? statusLine : "no response"));
    }
    discardHttpHeaders(rawIn);
    return s;
  }

  private static void discardHttpHeaders(InputStream rawIn) throws IOException {
    for (; ; ) {
      String header = readHttpLine(rawIn);
      if (header == null || header.isEmpty()) {
        return;
      }
    }
  }

  private static String readHttpLine(InputStream in) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (; ; ) {
      int b = in.read();
      if (b == -1) {
        if (sb.length() == 0) {
          return null;
        }
        return sb.toString();
      }
      if (b == '\r') {
        continue;
      }
      if (b == '\n') {
        return sb.toString();
      }
      sb.append((char) b);
    }
  }

  public void sendPacket(Packet packet) throws IOException {
    ensureConnected();
    int packetId = packet.getPacketId(protocolState.get());
    byte[] frame = PacketFraming.encode(packetId, packet);
    out.write(frame);
    out.flush();
    logger.debug("Sent packet id=0x{} size={}", Integer.toHexString(packetId), frame.length);
    eventBus.publishAsync(
        new PacketSentEvent(packetId, protocolState.get(), host, port, frame.length));
  }

  public Packet receivePacket(PacketRegistry registry) throws IOException {
    ensureConnected();
    PacketFraming.FramedPacket frame = PacketFraming.readFrame(in);
    int packetId = frame.getPacketId();
    PacketCodec codec = registry.getCodec(packetId);
    if (codec == null) {
      throw new PacketFraming.UnknownPacketException(
          "Unknown packet id: 0x"
              + Integer.toHexString(packetId)
              + " in state "
              + protocolState.get());
    }
    Packet packet = PacketFraming.decode(packetId, frame.getPayload(), codec);
    logger.debug(
        "Received packet id=0x{} size={}",
        Integer.toHexString(packetId),
        frame.getPayload().length + VarInt.size(packetId));
    eventBus.publishAsync(
        new PacketReceivedEvent(
            packetId, protocolState.get(), host, port, frame.getPayload().length));
    return packet;
  }

  public void setProtocolState(ProtocolState newState) {
    Objects.requireNonNull(newState, "newState");
    ProtocolState prev = protocolState.get();
    if (prev == newState) {
      return;
    }
    validateTransition(prev, newState);
    protocolState.set(newState);
    logger.info("Protocol state changed for {}:{}: {} -> {}", host, port, prev, newState);
    eventBus.publishAsync(new ProtocolStateChangedEvent(host, port, prev, newState));
  }

  @Override
  public ProtocolState getProtocolState() {
    return protocolState.get();
  }

  @Override
  public ConnectionState getState() {
    return state.get();
  }

  @Override
  public boolean isConnected() {
    return state.get() == ConnectionState.CONNECTED && socket != null && socket.isConnected();
  }

  @Override
  public void close() {
    ConnectionState prev = state.getAndSet(ConnectionState.CLOSED);
    if (prev == ConnectionState.CLOSED) {
      return;
    }
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      logger.warn("Error closing socket to {}:{}: {}", host, port, e.getMessage());
    }
    logger.info("Disconnected from {}:{}", host, port);
    eventBus.publishAsync(new ConnectionClosedEvent(host, port, prev, "connection closed"));
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void setReadTimeout(int readTimeout) throws IOException {
    if (readTimeout <= 0) {
      throw new IllegalArgumentException("readTimeout must be positive");
    }
    this.readTimeout = readTimeout;
    if (socket != null) {
      socket.setSoTimeout(readTimeout);
    }
  }

  private void ensureConnected() {
    if (state.get() != ConnectionState.CONNECTED) {
      throw new IllegalStateException("Not connected (state=" + state.get() + ")");
    }
  }

  private static void validateTransition(ProtocolState current, ProtocolState next) {
    boolean allowed =
        (current == ProtocolState.HANDSHAKE
                && (next == ProtocolState.STATUS || next == ProtocolState.LOGIN))
            || (current == ProtocolState.LOGIN && next == ProtocolState.CONFIGURATION)
            || (current == ProtocolState.CONFIGURATION && next == ProtocolState.PLAY);
    if (!allowed) {
      throw new IllegalStateException("Invalid protocol transition: " + current + " -> " + next);
    }
  }

  Socket getSocket() {
    return socket;
  }

  private static void closeQuietly(Socket s) {
    if (s != null) {
      try {
        s.close();
      } catch (IOException ignored) {
      }
    }
  }
}
