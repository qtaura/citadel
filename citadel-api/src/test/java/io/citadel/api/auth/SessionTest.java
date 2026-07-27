package io.citadel.api.auth;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionTest {

  @Test
  void sessionRequiresFields() {
    UUID profileId = UUID.randomUUID();
    assertThrows(
        NullPointerException.class,
        () ->
            new Session(
                null, profileId, "Steve", AccountType.OFFLINE, Optional.empty(), Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () ->
            new Session(
                "main", null, "Steve", AccountType.OFFLINE, Optional.empty(), Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () ->
            new Session(
                "main", profileId, null, AccountType.OFFLINE, Optional.empty(), Optional.empty()));
  }

  @Test
  void offlineSessionIsDeterministic() {
    Account account = Account.offline("Steve");

    Session first = Session.offline(account);
    Session second = Session.offline(account);

    assertEquals(first.profileId(), second.profileId());
    assertEquals("Steve", first.username());
    assertEquals(AccountType.OFFLINE, first.accountType());
    assertEquals(Optional.empty(), first.accessToken());
    assertEquals(Optional.empty(), first.expiresAt());
  }

  @Test
  void onlineSessionCanCarryTokenMetadata() {
    UUID profileId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plusSeconds(60);

    Session session =
        new Session(
            "main",
            profileId,
            "Steve",
            AccountType.MICROSOFT,
            Optional.of("token"),
            Optional.of(expiresAt));

    assertEquals(Optional.of("token"), session.accessToken());
    assertEquals(Optional.of(expiresAt), session.expiresAt());
  }
}
