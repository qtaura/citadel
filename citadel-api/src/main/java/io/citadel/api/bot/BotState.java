package io.citadel.api.bot;

public enum BotState {
  CREATED,
  STARTING,
  CONNECTING,
  AUTHENTICATING,
  RUNNING,
  RECONNECTING,
  STOPPING,
  STOPPED,
  FAILED
}
