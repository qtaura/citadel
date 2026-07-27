package io.citadel.api.event.account;

import io.citadel.api.account.Account;
import io.citadel.api.event.Event;
import java.util.Objects;

/** Published when a new account has been registered. */
public final class AccountRegisteredEvent extends Event {

  private final Account account;

  public AccountRegisteredEvent(Account account) {
    super(null, account.id());
    this.account = Objects.requireNonNull(account, "account");
  }

  public Account getAccount() {
    return account;
  }
}
