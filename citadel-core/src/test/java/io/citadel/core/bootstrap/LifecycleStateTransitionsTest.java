package io.citadel.core.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LifecycleStateTransitionsTest {

  @Test
  void initialStateIsCreated() {
    Lifecycle lifecycle = new Lifecycle();
    assertSame(LifecycleState.CREATED, lifecycle.getState());
  }

  @Test
  void startTransitionsToStarting() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    assertSame(LifecycleState.STARTING, lifecycle.getState());
  }

  @Test
  void finishStartupTransitionsToRunning() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.finishStartup();
    assertSame(LifecycleState.RUNNING, lifecycle.getState());
  }

  @Test
  void shutdownFromRunningTransitionsToTerminated() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.finishStartup();
    lifecycle.shutdown();
    assertSame(LifecycleState.TERMINATED, lifecycle.getState());
  }

  @Test
  void shutdownFromStartingTransitionsToTerminated() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.shutdown();
    assertSame(LifecycleState.TERMINATED, lifecycle.getState());
  }

  @Test
  void shutdownFromCreatedIsNoOp() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.shutdown();
    assertSame(LifecycleState.CREATED, lifecycle.getState());
  }

  @Test
  void multipleShutdownCallsAreIdempotent() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.finishStartup();
    lifecycle.shutdown();
    lifecycle.shutdown();
    assertSame(LifecycleState.TERMINATED, lifecycle.getState());
  }

  @Test
  void shutdownFromTerminatedIsNoOp() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.finishStartup();
    lifecycle.shutdown();
    lifecycle.shutdown();
    assertSame(LifecycleState.TERMINATED, lifecycle.getState());
  }
}
