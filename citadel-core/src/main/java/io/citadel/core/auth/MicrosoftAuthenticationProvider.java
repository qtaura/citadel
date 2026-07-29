package io.citadel.core.auth;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import io.citadel.api.auth.AuthenticationProvider;
import io.citadel.api.auth.Session;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import java.util.Objects;
import java.util.Optional;

public final class MicrosoftAuthenticationProvider implements AuthenticationProvider {

  private final MicrosoftAuthenticator authenticator;
  private final Logger logger;

  public MicrosoftAuthenticationProvider(Logger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.authenticator = new MicrosoftAuthenticator();
  }

  public MicrosoftAuthenticationProvider(Logger logger, Configuration config) {
    this.logger = Objects.requireNonNull(logger, "logger");
    ConfigurationSection azure = config.getSection("authentication");
    String clientId = azure.getString("azure_client_id", MicrosoftAuthenticator.DEFAULT_CLIENT_ID);
    String tenant = azure.getString("azure_tenant", MicrosoftAuthenticator.DEFAULT_TENANT);
    this.authenticator = new MicrosoftAuthenticator(clientId, tenant);
    if (!MicrosoftAuthenticator.DEFAULT_CLIENT_ID.equals(clientId)
        || !MicrosoftAuthenticator.DEFAULT_TENANT.equals(tenant)) {
      logger.info("Using custom Azure AD app: client_id={}, tenant={}", clientId, tenant);
    }
  }

  @Override
  public AccountType accountType() {
    return AccountType.MICROSOFT;
  }

  @Override
  public Session authenticate(Account account) {
    logger.info("Starting Microsoft OAuth for account {}", account.id());
    try {
      MicrosoftAuthenticator.OAuthToken oauthToken = authenticator.authenticateWithBrowser();
      MicrosoftAuthenticator.XblToken xblToken =
          authenticator.authenticateXbl(oauthToken.accessToken());
      MicrosoftAuthenticator.XstsToken xstsToken = authenticator.authenticateXsts(xblToken.token());
      MicrosoftAuthenticator.MinecraftToken mcToken =
          authenticator.loginMinecraft(xstsToken.uhs(), xstsToken.token());
      MicrosoftAuthenticator.MinecraftProfile profile =
          authenticator.lookupProfile(mcToken.accessToken());
      logger.info(
          "Microsoft authentication succeeded for account {} as {}",
          account.id(),
          profile.username());
      return new Session(
          account.id(),
          profile.profileId(),
          profile.username(),
          AccountType.MICROSOFT,
          Optional.of(mcToken.accessToken()),
          Optional.of(mcToken.expiresAt()));
    } catch (AuthenticationException e) {
      throw new RuntimeException(
          "Microsoft authentication failed for account " + account.id() + ": " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<Session> refresh(Session stale) {
    if (stale.accessToken().isEmpty()) {
      return Optional.empty();
    }
    if (stale.accountType() != AccountType.MICROSOFT) {
      return Optional.empty();
    }
    try {
      MicrosoftAuthenticator.OAuthToken refreshed =
          authenticator.refreshAccessToken(stale.accessToken().get());
      MicrosoftAuthenticator.XblToken xblToken =
          authenticator.authenticateXbl(refreshed.accessToken());
      MicrosoftAuthenticator.XstsToken xstsToken = authenticator.authenticateXsts(xblToken.token());
      MicrosoftAuthenticator.MinecraftToken mcToken =
          authenticator.loginMinecraft(xstsToken.uhs(), xstsToken.token());
      MicrosoftAuthenticator.MinecraftProfile profile =
          authenticator.lookupProfile(mcToken.accessToken());
      return Optional.of(
          new Session(
              stale.accountId(),
              profile.profileId(),
              profile.username(),
              AccountType.MICROSOFT,
              Optional.of(mcToken.accessToken()),
              Optional.of(mcToken.expiresAt())));
    } catch (AuthenticationException e) {
      logger.warn(
          "Failed to refresh session for account {}: {}", stale.accountId(), e.getMessage());
      return Optional.empty();
    }
  }
}
