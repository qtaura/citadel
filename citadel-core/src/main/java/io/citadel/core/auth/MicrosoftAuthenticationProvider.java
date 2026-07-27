package io.citadel.core.auth;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import io.citadel.api.auth.AuthenticationProvider;
import io.citadel.api.auth.Session;
import java.util.Optional;

/**
 * Placeholder for Microsoft OAuth authentication.
 *
 * <p>Implementation includes:
 *
 * <ol>
 *   <li>Device code OAuth flow
 *   <li>Mojang/Yggdrasil token exchange
 *   <li>Access token and refresh token management
 *   <li>Automatic token refresh before expiration
 * </ol>
 *
 * <p>This stub exists to validate the {@link AuthenticationProvider} contract. Real implementation
 * is added in a future milestone.
 */
public final class MicrosoftAuthenticationProvider implements AuthenticationProvider {

  private static final String NOT_IMPLEMENTED = "Microsoft authentication not yet implemented";

  @Override
  public AccountType accountType() {
    return AccountType.MICROSOFT;
  }

  @Override
  public Session authenticate(Account account) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public Optional<Session> refresh(Session stale) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }
}
