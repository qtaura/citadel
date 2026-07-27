package io.citadel.api.bot;

import io.citadel.api.service.Service;
import java.util.Collection;
import java.util.UUID;

/**
 * Manages bot lifecycles.
 *
 * <p>This service owns bot creation, destruction, and lifecycle coordination. It delegates to
 * AccountManager for account definitions, AuthenticationService for authentication, and
 * NetworkClient for networking.
 *
 * <p>Thread safety: implementations must be thread-safe.
 */
public interface BotManager extends Service {

  Bot create(String accountId);

  Bot get(UUID id);

  Collection<Bot> getAll();

  boolean destroy(UUID id);

  void startAll();

  void stopAll();

  int size();
}
