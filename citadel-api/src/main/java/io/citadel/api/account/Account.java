package io.citadel.api.account;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable account definition used throughout Citadel.
 *
 * <p>An account contains identity information ({@code id}, {@code username}, {@code type}) and
 * optional configuration metadata ({@code server}, {@code proxy}, {@code tags}, {@code enabled}).
 *
 * <p>Account objects are always immutable. Use the 3-parameter constructor for simple cases, or the
 * full constructor / {@link #builder()} for metadata-rich definitions.
 */
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName", "PMD.ShortMethodName"})
public record Account(
    String id,
    String username,
    AccountType type,
    Optional<String> server,
    Optional<String> proxy,
    List<String> tags,
    boolean enabled) {

  public Account {
    id = requireText(id, "id");
    username = requireText(username, "username");
    type = Objects.requireNonNull(type, "type");
    Objects.requireNonNull(server, "server");
    Objects.requireNonNull(proxy, "proxy");
    tags = List.copyOf(tags);
  }

  public Account(String id, String username, AccountType type) {
    this(id, username, type, Optional.empty(), Optional.empty(), List.of(), true);
  }

  public static Account offline(String username) {
    return new Account(username, username, AccountType.OFFLINE);
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  public static final class Builder {
    private String id;
    private String username;
    private AccountType type;
    private Optional<String> server = Optional.empty();
    private Optional<String> proxy = Optional.empty();
    private List<String> tags = List.of();
    private boolean enabled = true;

    private Builder() {}

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder type(AccountType type) {
      this.type = type;
      return this;
    }

    public Builder server(String server) {
      this.server = server != null ? Optional.of(server) : Optional.empty();
      return this;
    }

    public Builder proxy(String proxy) {
      this.proxy = proxy != null ? Optional.of(proxy) : Optional.empty();
      return this;
    }

    public Builder tags(List<String> tags) {
      this.tags = tags != null ? tags : List.of();
      return this;
    }

    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Account build() {
      return new Account(id, username, type, server, proxy, tags, enabled);
    }
  }
}
