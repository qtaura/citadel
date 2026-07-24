package io.citadel.api.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Base class for all Citadel events.
 *
 * <p>Every event published through the {@link EventBus} must extend this class. Events are
 * immutable value objects — once constructed, their state cannot be modified.
 *
 * <p>Subclasses should follow the same immutability pattern: all fields must be final, collections
 * must be unmodifiable, and the class should be declared final unless designed for extension.
 *
 * <p>Events are not serializable by default. If serialization is needed, subclasses should
 * implement their own mechanism.
 *
 * <p>Thread safety: this class is thread-safe (immutable). Subclasses must also be thread-safe.
 */
public abstract class Event {

  private final Instant timestamp;
  private final String sourcePlugin;
  private final String accountId;

  /** Constructs a new event with the current system time as the timestamp. */
  protected Event() {
    this.timestamp = Instant.now();
    this.sourcePlugin = null;
    this.accountId = null;
  }

  /**
   * Constructs a new event with a specific timestamp.
   *
   * @param timestamp the event timestamp (not null)
   * @throws NullPointerException if timestamp is null
   */
  protected Event(Instant timestamp) {
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    this.sourcePlugin = null;
    this.accountId = null;
  }

  /**
   * Constructs a new event with source and account metadata.
   *
   * @param sourcePlugin the name of the plugin that published this event, or null
   * @param accountId the account identifier associated with this event, or null
   */
  protected Event(String sourcePlugin, String accountId) {
    this.timestamp = Instant.now();
    this.sourcePlugin = sourcePlugin;
    this.accountId = accountId;
  }

  /**
   * Returns the time at which this event was created.
   *
   * @return the event timestamp (never null)
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Returns the name of the plugin that published this event.
   *
   * @return the source plugin name, or null if no source was set
   */
  public String getSourcePlugin() {
    return sourcePlugin;
  }

  /**
   * Returns the account identifier associated with this event.
   *
   * @return the account id, or null if no account was associated
   */
  public String getAccountId() {
    return accountId;
  }
}
