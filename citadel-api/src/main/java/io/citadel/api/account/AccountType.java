package io.citadel.api.account;

/** Identifies how an account obtains Minecraft session credentials. */
public enum AccountType {
  /** Offline-mode account; no Microsoft session is required. */
  OFFLINE,

  /** Microsoft account; OAuth and token exchange are added in a later milestone. */
  MICROSOFT,

  /** Account backed by a previously acquired cached session. */
  CACHED_SESSION
}
