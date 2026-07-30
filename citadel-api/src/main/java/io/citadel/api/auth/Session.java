package io.citadel.api.auth;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable authenticated Minecraft session. */
public record Session(
    String accountId,
    UUID profileId,
    String username,
    AccountType accountType,
    Optional<String> accessToken,
    Optional<Instant> expiresAt,
    Optional<byte[]> keyPair) {

  /** Creates a validated session value. */
  public Session {
    accountId = requireText(accountId, "accountId");
    profileId = Objects.requireNonNull(profileId, "profileId");
    username = requireText(username, "username");
    accountType = Objects.requireNonNull(accountType, "accountType");
    accessToken = Objects.requireNonNull(accessToken, "accessToken");
    expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    keyPair = Objects.requireNonNull(keyPair, "keyPair");
  }

  public Session(
      String accountId,
      UUID profileId,
      String username,
      AccountType accountType,
      Optional<String> accessToken,
      Optional<Instant> expiresAt) {
    this(accountId, profileId, username, accountType, accessToken, expiresAt, Optional.empty());
  }

  /** Creates a deterministic offline session for an account. */
  public static Session offline(Account account) {
    Objects.requireNonNull(account, "account");
    UUID offlineUuid =
        UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + account.username()).getBytes(StandardCharsets.UTF_8));
    return new Session(
        account.id(),
        offlineUuid,
        account.username(),
        account.type(),
        Optional.empty(),
        Optional.empty());
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
