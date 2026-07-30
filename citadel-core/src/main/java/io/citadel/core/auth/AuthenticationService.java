package io.citadel.core.auth;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import io.citadel.api.auth.Session;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.auth.LoginFailedEvent;
import io.citadel.api.event.auth.LoginStartedEvent;
import io.citadel.api.event.auth.LoginSucceededEvent;
import io.citadel.api.network.ProtocolState;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.Logger;
import io.citadel.core.net.Connection;
import io.citadel.core.net.NetworkClient;
import io.citadel.core.net.Packet;
import io.citadel.core.net.PacketRegistry;
import io.citadel.core.net.protocol.DisconnectPacket;
import io.citadel.core.net.protocol.EncryptionRequestPacket;
import io.citadel.core.net.protocol.EncryptionResponsePacket;
import io.citadel.core.net.protocol.HandshakePacket;
import io.citadel.core.net.protocol.LoginProtocolCodecs;
import io.citadel.core.net.protocol.LoginStartPacket;
import io.citadel.core.net.protocol.LoginSuccessPacket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Objects;

public final class AuthenticationService {

  private static final int DEFAULT_LOGIN_TIMEOUT = 10000;

  private final EventBus eventBus;
  private final Logger logger;
  private final boolean offlineMode;
  private final int loginTimeout;
  private final PacketRegistry loginRegistry;
  private final SessionServerClient sessionServerClient;

  public AuthenticationService(EventBus eventBus, Logger logger, Configuration config) {
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    Objects.requireNonNull(config, "config");
    this.offlineMode = config.getBoolean("authentication.offline_mode", true);
    this.loginTimeout = config.getInt("connection.login_timeout", DEFAULT_LOGIN_TIMEOUT);
    this.loginRegistry =
        PacketRegistry.builder()
            .register(0x00, LoginProtocolCodecs.disconnect())
            .register(0x01, LoginProtocolCodecs.encryptionRequest())
            .register(0x02, LoginProtocolCodecs.loginSuccess())
            .build();
    this.sessionServerClient = new SessionServerClient();
  }

  public Session login(Connection connection, Account account) throws AuthenticationException {
    return login(connection, account, Session.offline(account));
  }

  public Session login(Connection connection, Account account, Session candidate)
      throws AuthenticationException {
    Objects.requireNonNull(connection, "connection");
    Objects.requireNonNull(account, "account");
    Objects.requireNonNull(candidate, "candidate");

    logger.info(
        "Starting login for account {} to {}:{}",
        account.id(),
        connection.getHost(),
        connection.getPort());
    eventBus.publishAsync(
        new LoginStartedEvent(
            account.id(), account.username(), connection.getHost(), connection.getPort()));

    try {
      validateAccount(account);
    } catch (AuthenticationException e) {
      throw fail(connection, account, e.getMessage(), e);
    }

    try {
      connection.setReadTimeout(loginTimeout);
      System.out.println("[LOGIN] Sending Handshake...");
      System.out.flush();
      connection.sendPacket(
          new HandshakePacket(
              NetworkClient.MINECRAFT_PROTOCOL_VERSION,
              connection.getHost(),
              connection.getPort(),
              2));
      connection.setProtocolState(ProtocolState.LOGIN);
      System.out.println("[LOGIN] Sending LoginStart...");
      System.out.flush();
      connection.sendPacket(new LoginStartPacket(candidate.username(), candidate.profileId()));
      System.out.println("[LOGIN] Waiting for server response...");
      System.out.flush();
      return handleLoginSequence(connection, account, candidate);
    } catch (SocketTimeoutException e) {
      throw fail(connection, account, "Login timed out", e);
    } catch (IOException | RuntimeException e) {
      throw fail(connection, account, "Login failed: " + e.getMessage(), e);
    }
  }

  public boolean isOfflineMode() {
    return offlineMode;
  }

  public int getLoginTimeout() {
    return loginTimeout;
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private Session handleLoginSequence(Connection connection, Account account, Session candidate)
      throws IOException, AuthenticationException {
    System.out.println("[LOGIN] Receiving first login packet...");
    System.out.flush();
    Packet firstPacket = connection.receivePacket(loginRegistry);

    if (firstPacket instanceof DisconnectPacket disconnect) {
      throw fail(
          connection,
          account,
          "Server disconnected during login: " + disconnect.getReasonJson(),
          null);
    }

    if (firstPacket instanceof EncryptionRequestPacket encryptReq) {
      handleEncryption(connection, account, candidate, encryptReq);
    }

    if (firstPacket instanceof EncryptionRequestPacket) {
      System.out.println("[LOGIN] Waiting for LoginSuccess after encryption...");
      System.out.flush();
      Packet loginPacket = connection.receivePacket(loginRegistry);
      if (loginPacket instanceof DisconnectPacket disconnect) {
        String reason = disconnect.getReasonJson();
        System.out.println("[LOGIN] Server disconnected: " + reason);
        System.out.flush();
        throw fail(connection, account, "Server disconnected: " + reason, null);
      }
      if (!(loginPacket instanceof LoginSuccessPacket success)) {
        throw fail(
            connection,
            account,
            "Unexpected login packet: " + loginPacket.getClass().getSimpleName(),
            null);
      }
      return finalizeLogin(connection, account, candidate, success);
    }

    if (firstPacket instanceof LoginSuccessPacket success) {
      return finalizeLogin(connection, account, candidate, success);
    }

    throw fail(
        connection,
        account,
        "Unexpected login packet: " + firstPacket.getClass().getSimpleName(),
        null);
  }

  private void handleEncryption(
      Connection connection, Account account, Session candidate, EncryptionRequestPacket encryptReq)
      throws IOException, AuthenticationException {
    EncryptionHandler encHandler =
        new EncryptionHandler(encryptReq.getPublicKey(), encryptReq.getVerifyToken());
    connection.sendPacket(
        new EncryptionResponsePacket(
            encHandler.getEncryptedSharedSecret(), encHandler.getEncryptedVerifyToken()));
    connection.enableEncryption(encHandler.getSharedSecret());
    String serverId = encHandler.computeServerId();
    System.out.println("[AUTH] Server ID hash: " + serverId);
    System.out.println("[AUTH] Profile ID: " + candidate.profileId());
    System.out.println("[AUTH] Access token available: " + candidate.accessToken().isPresent());
    System.out.flush();
    if (candidate.accessToken().isPresent()) {
      String token = candidate.accessToken().get();
      System.out.println("[AUTH] Token (" + token.length() + " chars): " + token.substring(0, Math.min(40, token.length())) + "...");
      System.out.flush();
      sessionServerClient.joinServer(token, candidate.profileId(), serverId);
      System.out.println("[AUTH] Session server join completed successfully");
      System.out.flush();
    } else {
      logger.warn(
          "Server requires encryption but no access token available (account: {})", account.id());
    }
  }

  private Session finalizeLogin(
      Connection connection, Account account, Session candidate, LoginSuccessPacket success) {
    connection.setProtocolState(ProtocolState.CONFIGURATION);
    Session session =
        new Session(
            account.id(),
            success.getProfileId(),
            success.getUsername(),
            account.type(),
            candidate.accessToken(),
            candidate.expiresAt());
    logger.info("Login succeeded for account {} as {}", account.id(), session.username());
    eventBus.publishAsync(
        new LoginSucceededEvent(
            account.id(),
            session.username(),
            session.profileId(),
            connection.getHost(),
            connection.getPort()));
    return session;
  }

  private void validateAccount(Account account) throws AuthenticationException {
    if (account.type() == AccountType.OFFLINE) {
      if (!offlineMode) {
        throw new AuthenticationException("Offline accounts are disabled by configuration");
      }
      return;
    }
    if (account.type() == AccountType.MICROSOFT || account.type() == AccountType.CACHED_SESSION) {
      return;
    }
    throw new AuthenticationException(account.type() + " authentication is not implemented yet");
  }

  private AuthenticationException fail(
      Connection connection, Account account, String reason, Throwable cause) {
    logger.warn("Login failed for account {}: {}", account.id(), reason);
    eventBus.publishAsync(
        new LoginFailedEvent(
            account.id(), account.username(), reason, connection.getHost(), connection.getPort()));
    connection.close();
    if (cause == null) {
      return new AuthenticationException(reason);
    }
    return new AuthenticationException(reason, cause);
  }
}
