package io.citadel.core.net;

import io.citadel.api.event.EventBus;
import io.citadel.api.event.network.ConnectionClosedEvent;
import io.citadel.api.event.network.ConnectionOpenedEvent;
import io.citadel.api.event.network.PacketReceivedEvent;
import io.citadel.api.event.network.PacketSentEvent;
import io.citadel.api.event.network.ProtocolStateChangedEvent;
import io.citadel.api.network.ConnectionState;
import io.citadel.api.network.ProtocolState;
import io.citadel.api.service.Logger;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Connection implements Closeable {

  private final AtomicReference<ConnectionState> state;
  private final AtomicReference<ProtocolState> protocolState;
  private final String host;
  private final int port;
  private final int connectTimeout;
  private final int readTimeout;
  private final EventBus eventBus;
  private final Logger logger;

  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;

  Connection(
      String host,
      int port,
      int connectTimeout,
      int readTimeout,
      EventBus eventBus,
      Logger logger) {
    this.state = new AtomicReference<>(ConnectionState.CREATED);
    this.protocolState = new AtomicReference<>(ProtocolState.HANDSHAKE);
    this.host = host;
    this.port = port;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.eventBus = eventBus;
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public void connect() throws IOException {
    if (!state.compareAndSet(ConnectionState.CREATED, ConnectionState.CONNECTING)) {
      throw new IllegalStateException("Cannot connect from state: " + state.get());
    }
    ConnectionState prev = state.get();
    logger.info("Connecting to {}:{} ...", host, port);
    try {
      Socket s = new Socket();
      s.connect(new InetSocketAddress(host, port), connectTimeout);
      s.setSoTimeout(readTimeout);
      this.socket = s;
      this.in = new DataInputStream(s.getInputStream());
      this.out = new DataOutputStream(s.getOutputStream());
      state.set(ConnectionState.CONNECTED);
      logger.info("Connected to {}:{}", host, port);
      eventBus.publishAsync(new ConnectionOpenedEvent(host, port, prev));
    } catch (IOException e) {
      state.set(ConnectionState.CLOSED);
      logger.error("Failed to connect to {}:{}: {}", host, port, e.getMessage());
      throw e;
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

  public ProtocolState getProtocolState() {
    return protocolState.get();
  }

  public ConnectionState getState() {
    return state.get();
  }

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

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
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
}
