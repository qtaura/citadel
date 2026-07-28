package io.citadel.api.bot;

import io.citadel.api.account.Account;
import io.citadel.api.auth.Session;
import io.citadel.api.network.Connection;
import io.citadel.api.world.WorldManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a single running Minecraft client.
 *
 * <p>A Bot owns the runtime lifecycle of a connected client, orchestrating existing components
 * (AccountManager, NetworkClient, AuthenticationService) without duplicating their
 * responsibilities.
 *
 * <p>Thread safety: implementations must be thread-safe.
 */
public interface Bot {

  UUID getBotId();

  Account getAccount();

  Connection getConnection();

  Session getSession();

  WorldManager getWorld();

  BotState getState();

  boolean isRunning();

  CompletableFuture<Void> start();

  CompletableFuture<Void> stop();

  CompletableFuture<Void> restart();
}
