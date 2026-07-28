package io.citadel.api.reconnect;

import java.time.Duration;

public record ReconnectPolicy(
    boolean enabled, int maxAttempts, long initialDelayMs, long maxDelayMs) {

  public ReconnectPolicy {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be >= 1, got: " + maxAttempts);
    }
    if (initialDelayMs < 0) {
      throw new IllegalArgumentException("initialDelayMs must be >= 0, got: " + initialDelayMs);
    }
    if (maxDelayMs < initialDelayMs) {
      throw new IllegalArgumentException("maxDelayMs must be >= initialDelayMs");
    }
  }

  public Duration delayForAttempt(int attempt) {
    if (attempt < 0) {
      throw new IllegalArgumentException("attempt must be >= 0, got: " + attempt);
    }
    long delay = (long) (initialDelayMs * Math.pow(2, attempt));
    return Duration.ofMillis(Math.min(delay, maxDelayMs));
  }

  public static ReconnectPolicy disabled() {
    return new ReconnectPolicy(false, 1, 0, 0);
  }

  public static ReconnectPolicy defaults() {
    return new ReconnectPolicy(true, 10, 2_000, 60_000);
  }
}
