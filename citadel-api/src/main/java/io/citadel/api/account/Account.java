package io.citadel.api.account;

import java.util.Objects;

/**
 * Immutable account identity used by authentication and connection workflows.
 *
 * <p>The account does not expose credentials directly. Future authentication providers can attach
 * credential references outside this value without coupling networking to authentication details.
 */
public record Account(String id, String username, AccountType type) {

  /** Creates a validated account value. */
  public Account {
    id = requireText(id, "id");
    username = requireText(username, "username");
    type = Objects.requireNonNull(type, "type");
  }

  /** Creates an offline-mode account with the same id and username. */
  public static Account offline(String username) {
    return new Account(username, username, AccountType.OFFLINE);
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
