package io.citadel.core.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LifecycleStateTest {

  @Test
  void createdToStarting() {
    assertTrue(LifecycleState.CREATED.canTransitionTo(LifecycleState.STARTING));
  }

  @Test
  void startingToRunning() {
    assertTrue(LifecycleState.STARTING.canTransitionTo(LifecycleState.RUNNING));
  }

  @Test
  void startingToShuttingDown() {
    assertTrue(LifecycleState.STARTING.canTransitionTo(LifecycleState.SHUTTING_DOWN));
  }

  @Test
  void runningToShuttingDown() {
    assertTrue(LifecycleState.RUNNING.canTransitionTo(LifecycleState.SHUTTING_DOWN));
  }

  @Test
  void shuttingDownToTerminated() {
    assertTrue(LifecycleState.SHUTTING_DOWN.canTransitionTo(LifecycleState.TERMINATED));
  }

  @Test
  void terminatedCannotTransition() {
    assertFalse(LifecycleState.TERMINATED.canTransitionTo(LifecycleState.CREATED));
    assertFalse(LifecycleState.TERMINATED.canTransitionTo(LifecycleState.STARTING));
    assertFalse(LifecycleState.TERMINATED.canTransitionTo(LifecycleState.RUNNING));
    assertFalse(LifecycleState.TERMINATED.canTransitionTo(LifecycleState.SHUTTING_DOWN));
    assertFalse(LifecycleState.TERMINATED.canTransitionTo(LifecycleState.TERMINATED));
  }

  @Test
  void invalidTransitions() {
    assertFalse(LifecycleState.CREATED.canTransitionTo(LifecycleState.RUNNING));
    assertFalse(LifecycleState.CREATED.canTransitionTo(LifecycleState.SHUTTING_DOWN));
    assertFalse(LifecycleState.CREATED.canTransitionTo(LifecycleState.TERMINATED));
    assertFalse(LifecycleState.CREATED.canTransitionTo(LifecycleState.CREATED));
    assertFalse(LifecycleState.RUNNING.canTransitionTo(LifecycleState.CREATED));
    assertFalse(LifecycleState.RUNNING.canTransitionTo(LifecycleState.STARTING));
    assertFalse(LifecycleState.RUNNING.canTransitionTo(LifecycleState.RUNNING));
    assertFalse(LifecycleState.SHUTTING_DOWN.canTransitionTo(LifecycleState.CREATED));
    assertFalse(LifecycleState.SHUTTING_DOWN.canTransitionTo(LifecycleState.STARTING));
    assertFalse(LifecycleState.SHUTTING_DOWN.canTransitionTo(LifecycleState.RUNNING));
    assertFalse(LifecycleState.SHUTTING_DOWN.canTransitionTo(LifecycleState.SHUTTING_DOWN));
  }
}
