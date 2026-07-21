package io.citadel.core.bootstrap;

/**
 * States in the Citadel application lifecycle.
 *
 * <p>Legal transitions:
 *
 * <ul>
 *   <li>{@link #CREATED} → {@link #STARTING}
 *   <li>{@link #STARTING} → {@link #RUNNING} | {@link #SHUTTING_DOWN}
 *   <li>{@link #RUNNING} → {@link #SHUTTING_DOWN}
 *   <li>{@link #SHUTTING_DOWN} → {@link #TERMINATED}
 * </ul>
 */
public enum LifecycleState {
  CREATED,
  STARTING,
  RUNNING,
  SHUTTING_DOWN,
  TERMINATED;

  /**
   * Returns true if this state may legally transition to the given target state.
   *
   * @param target the desired next state
   * @return true if the transition is legal
   */
  public boolean canTransitionTo(LifecycleState target) {
    return switch (this) {
      case CREATED -> target == STARTING;
      case STARTING -> target == RUNNING || target == SHUTTING_DOWN;
      case RUNNING -> target == SHUTTING_DOWN;
      case SHUTTING_DOWN -> target == TERMINATED;
      case TERMINATED -> false;
    };
  }
}
