package io.citadel.api.event.account;

import io.citadel.api.account.Account;
import io.citadel.api.event.Event;
import java.util.Objects;

/** Published when an account has been removed. */
public final class AccountRemovedEvent extends Event {

  private final Account account;

  public AccountRemovedEvent(Account account) {
    super(null, account.id());
    this.account = Objects.requireNonNull(account, "account");
  }

  public Account getAccount() {
    return account;
  }
}
