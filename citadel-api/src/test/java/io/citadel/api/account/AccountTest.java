package io.citadel.api.account;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccountTest {

  @Test
  void accountRequiresId() {
    assertThrows(NullPointerException.class, () -> new Account(null, "Steve", AccountType.OFFLINE));
    assertThrows(
        IllegalArgumentException.class, () -> new Account(" ", "Steve", AccountType.OFFLINE));
  }

  @Test
  void accountRequiresUsername() {
    assertThrows(NullPointerException.class, () -> new Account("main", null, AccountType.OFFLINE));
    assertThrows(
        IllegalArgumentException.class, () -> new Account("main", " ", AccountType.OFFLINE));
  }

  @Test
  void offlineFactoryUsesUsernameAsId() {
    Account account = Account.offline("Steve");

    assertEquals("Steve", account.id());
    assertEquals("Steve", account.username());
    assertEquals(AccountType.OFFLINE, account.type());
  }
}
