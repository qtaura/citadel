package io.citadel.core.bot;

import io.citadel.api.bot.Bot;
import io.citadel.api.bot.BotManager;
import io.citadel.api.event.EventBus;
import io.citadel.api.event.bot.BotCreatedEvent;
import io.citadel.api.service.AccountManager;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.Logger;
import io.citadel.core.auth.AuthenticationService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BotManagerImpl implements BotManager {

  private final ConcurrentHashMap<UUID, Bot> bots;
  private final AccountManager accountManager;
  private final AuthenticationService authenticationService;
  private final EventBus eventBus;
  private final Logger logger;
  private final Configuration config;

  public BotManagerImpl(
      AccountManager accountManager,
      AuthenticationService authenticationService,
      EventBus eventBus,
      Logger logger,
      Configuration config) {
    this.bots = new ConcurrentHashMap<>();
    this.accountManager = Objects.requireNonNull(accountManager, "accountManager");
    this.authenticationService = authenticationService;
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public Bot create(String accountId) {
    Objects.requireNonNull(accountId, "accountId");
    if (!accountManager.contains(accountId)) {
      throw new IllegalArgumentException("Account not found: " + accountId);
    }
    BotImpl bot =
        new BotImpl(accountId, accountManager, authenticationService, eventBus, logger, config);
    bots.put(bot.getBotId(), bot);
    eventBus.publishAsync(new BotCreatedEvent(bot.getBotId(), accountId));
    return bot;
  }

  @Override
  public Bot get(UUID id) {
    Objects.requireNonNull(id, "id");
    return bots.get(id);
  }

  @Override
  public Collection<Bot> getAll() {
    return List.copyOf(bots.values());
  }

  @Override
  public boolean destroy(UUID id) {
    Objects.requireNonNull(id, "id");
    Bot bot = bots.remove(id);
    if (bot == null) {
      return false;
    }
    bot.stop();
    return true;
  }

  @Override
  public void startAll() {
    for (Bot bot : bots.values()) {
      bot.start();
    }
  }

  @Override
  public void stopAll() {
    for (Bot bot : bots.values()) {
      bot.stop();
    }
  }

  @Override
  public int size() {
    return bots.size();
  }
}
