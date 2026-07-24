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
import io.citadel.core.net.protocol.HandshakePacket;
import io.citadel.core.net.protocol.LoginProtocolCodecs;
import io.citadel.core.net.protocol.LoginStartPacket;
import io.citadel.core.net.protocol.LoginSuccessPacket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.Optional;

public final class AuthenticationService {

  private static final int DEFAULT_LOGIN_TIMEOUT = 10000;

  private final EventBus eventBus;
  private final Logger logger;
  private final boolean offlineMode;
  private final int loginTimeout;
  private final PacketRegistry loginRegistry;

  public AuthenticationService(EventBus eventBus, Logger logger, Configuration config) {
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    Objects.requireNonNull(config, "config");
    this.offlineMode = config.getBoolean("authentication.offline_mode", true);
    this.loginTimeout = config.getInt("connection.login_timeout", DEFAULT_LOGIN_TIMEOUT);
    this.loginRegistry =
        PacketRegistry.builder()
            .register(0x00, LoginProtocolCodecs.disconnect())
            .register(0x02, LoginProtocolCodecs.loginSuccess())
            .build();
  }

  public Session login(Connection connection, Account account) throws AuthenticationException {
    Objects.requireNonNull(connection, "connection");
    Objects.requireNonNull(account, "account");

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
      Session candidate = Session.offline(account);
      connection.sendPacket(
          new HandshakePacket(
              NetworkClient.MINECRAFT_PROTOCOL_VERSION,
              connection.getHost(),
              connection.getPort(),
              2));
      connection.setProtocolState(ProtocolState.LOGIN);
      connection.sendPacket(new LoginStartPacket(candidate.username(), candidate.profileId()));
      return handleLoginResponse(connection, account);
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

  private Session handleLoginResponse(Connection connection, Account account)
      throws IOException, AuthenticationException {
    Packet response = connection.receivePacket(loginRegistry);
    if (response instanceof DisconnectPacket disconnect) {
      throw fail(
          connection,
          account,
          "Server disconnected during login: " + disconnect.getReasonJson(),
          null);
    }
    if (!(response instanceof LoginSuccessPacket success)) {
      throw fail(
          connection,
          account,
          "Unexpected login packet: " + response.getClass().getSimpleName(),
          null);
    }

    connection.setProtocolState(ProtocolState.CONFIGURATION);
    Session session =
        new Session(
            account.id(),
            success.getProfileId(),
            success.getUsername(),
            account.type(),
            Optional.empty(),
            Optional.empty());
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
