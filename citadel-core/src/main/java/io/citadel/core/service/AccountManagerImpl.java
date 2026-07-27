package io.citadel.core.service;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.account.AccountRegisteredEvent;
import io.citadel.api.event.account.AccountRemovedEvent;
import io.citadel.api.event.account.AccountUpdatedEvent;
import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationException;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
public final class AccountManagerImpl implements AccountManager {

  private static final Set<String> KNOWN_ACCOUNT_FIELDS =
      Set.of("username", "type", "proxy", "server", "tags", "enabled");

  private final Map<String, Account> accounts;
  private final EventBus eventBus;
  private final Logger logger;

  public AccountManagerImpl(Configuration config, EventBus eventBus, Logger logger) {
    this.accounts = new ConcurrentHashMap<>();
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    Objects.requireNonNull(config, "config");
    loadAccounts(config);
    config.addListener(this::onReload);
  }

  @Override
  public Account get(String id) {
    Objects.requireNonNull(id, "id");
    return accounts.get(id);
  }

  @Override
  public List<Account> getAll() {
    return List.copyOf(accounts.values());
  }

  @Override
  public Stream<Account> stream() {
    return accounts.values().stream();
  }

  @Override
  public boolean register(Account account) {
    Objects.requireNonNull(account, "account");
    Account existing = accounts.putIfAbsent(account.id(), account);
    if (existing != null) {
      return false;
    }
    logger.info("Registered account {}", account.id());
    eventBus.publishAsync(new AccountRegisteredEvent(account));
    return true;
  }

  @Override
  public boolean unregister(String id) {
    Objects.requireNonNull(id, "id");
    Account removed = accounts.remove(id);
    if (removed == null) {
      return false;
    }
    logger.info("Unregistered account {}", id);
    eventBus.publishAsync(new AccountRemovedEvent(removed));
    return true;
  }

  @Override
  public boolean contains(String id) {
    Objects.requireNonNull(id, "id");
    return accounts.containsKey(id);
  }

  @Override
  public int size() {
    return accounts.size();
  }

  @Override
  public List<Account> findEnabled() {
    return accounts.values().stream().filter(Account::enabled).toList();
  }

  @Override
  public List<Account> findByTag(String tag) {
    Objects.requireNonNull(tag, "tag");
    return accounts.values().stream().filter(a -> a.tags().contains(tag)).toList();
  }

  @Override
  public List<Account> findByServer(String server) {
    Objects.requireNonNull(server, "server");
    return accounts.values().stream().filter(a -> server.equals(a.server().orElse(null))).toList();
  }

  private void onReload(Configuration config) {
    Map<String, Account> parsed = parseAll(config);
    for (Map.Entry<String, Account> entry : parsed.entrySet()) {
      Account existing = accounts.put(entry.getKey(), entry.getValue());
      if (existing != null && !existing.equals(entry.getValue())) {
        logger.info("Account {} updated during reload", entry.getKey());
        eventBus.publishAsync(new AccountUpdatedEvent(existing, entry.getValue()));
      }
    }
    accounts.keySet().retainAll(parsed.keySet());
  }

  private void loadAccounts(Configuration config) {
    for (Map.Entry<String, Account> entry : parseAll(config).entrySet()) {
      accounts.put(entry.getKey(), entry.getValue());
    }
  }

  private Map<String, Account> parseAll(Configuration config) {
    ConfigurationSection section = config.getSection("accounts");
    validateAll(section);
    Map<String, Account> result = new LinkedHashMap<>();
    for (String id : section.getKeys()) {
      ConfigurationSection def = section.getSection(id);
      Account account = parseAccount(id, def);
      result.put(account.id(), account);
    }
    return result;
  }

  private static void validateAll(ConfigurationSection section) {
    for (String id : section.getKeys()) {
      ConfigurationSection def = section.getSection(id);
      validateFields(def);
      def.getString("username");
      def.getEnum("type", AccountType.class);
    }
  }

  private static Account parseAccount(String id, ConfigurationSection section) {
    String username = section.getString("username");
    AccountType type = section.getEnum("type", AccountType.class);
    String proxy = section.getString("proxy", null);
    String server = section.getString("server", null);
    List<String> tags = section.contains("tags") ? section.getStringList("tags") : List.of();
    boolean enabled = section.getBoolean("enabled", true);
    return Account.builder()
        .id(id)
        .username(username)
        .type(type)
        .proxy(proxy)
        .server(server)
        .tags(tags)
        .enabled(enabled)
        .build();
  }

  private static void validateFields(ConfigurationSection section) {
    for (String key : section.getKeys()) {
      if (!KNOWN_ACCOUNT_FIELDS.contains(key)) {
        throw new ConfigurationException(
            key,
            "Unknown configuration field '"
                + key
                + "'. Valid fields: "
                + String.join(", ", KNOWN_ACCOUNT_FIELDS));
      }
    }
  }
}
