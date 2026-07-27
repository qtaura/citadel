package io.citadel.api.auth;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import java.util.Optional;

/**
 * Provides authenticated {@link Session} values for an account.
 *
 * <p>Each implementation handles a specific {@link AccountType}:
 *
 * <ul>
 *   <li>{@link AccountType#OFFLINE} — deterministic offline sessions (already implemented)
 *   <li>{@link AccountType#MICROSOFT} — Microsoft OAuth token exchange (placeholder)
 *   <li>{@link AccountType#CACHED_SESSION} — previously stored session replay (placeholder)
 * </ul>
 *
 * <p>This is a placeholder interface. The Microsoft provider is added in a future milestone.
 */
public interface AuthenticationProvider {

  AccountType accountType();

  Session authenticate(Account account);

  Optional<Session> refresh(Session stale);
}
