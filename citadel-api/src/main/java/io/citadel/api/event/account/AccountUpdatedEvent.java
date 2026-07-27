package io.citadel.api.event.account;

import io.citadel.api.account.Account;
import io.citadel.api.event.Event;
import java.util.Objects;

/** Published when an existing account definition has been updated. */
public final class AccountUpdatedEvent extends Event {

  private final Account oldAccount;
  private final Account newAccount;

  public AccountUpdatedEvent(Account oldAccount, Account newAccount) {
    super(null, newAccount.id());
    this.oldAccount = Objects.requireNonNull(oldAccount, "oldAccount");
    this.newAccount = Objects.requireNonNull(newAccount, "newAccount");
  }

  public Account getOldAccount() {
    return oldAccount;
  }

  public Account getNewAccount() {
    return newAccount;
  }
}
