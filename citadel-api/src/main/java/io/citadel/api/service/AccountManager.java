package io.citadel.api.service;

import io.citadel.api.account.Account;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages Minecraft account definitions and their lifecycle.
 *
 * <p>This service owns account definitions — it is the central point for registering, querying, and
 * removing accounts. Account definitions are loaded from YAML configuration at startup and can be
 * augmented at runtime.
 *
 * <p>Authentication is handled by {@code AuthenticationService}, not by this manager. Runtime bot
 * lifecycles are handled by a future {@code BotManager}.
 *
 * <p>Thread safety: implementations must be thread-safe. Returned collections must be immutable.
 */
public interface AccountManager extends Service {

  /** Returns the account with the given id, or null if not found. */
  Account get(String id);

  /** Returns an immutable snapshot of all registered accounts. */
  List<Account> getAll();

  /** Returns a stream of all registered accounts. */
  Stream<Account> stream();

  /**
   * Registers the given account.
   *
   * @return true if the account was registered, false if the id is already taken
   * @throws NullPointerException if account is null
   * @throws IllegalArgumentException if the account has blank id, blank username, or null type
   */
  boolean register(Account account);

  /**
   * Removes the account with the given id.
   *
   * @return true if an account was removed, false if no account had that id
   * @throws NullPointerException if id is null
   */
  boolean unregister(String id);

  /** Returns true if an account with the given id exists. */
  boolean contains(String id);

  /** Returns the number of registered accounts. */
  int size();

  /** Returns accounts that have {@code enabled == true}. */
  List<Account> findEnabled();

  /** Returns accounts that have the given tag. */
  List<Account> findByTag(String tag);

  /** Returns accounts whose server matches the given value. */
  List<Account> findByServer(String server);
}
