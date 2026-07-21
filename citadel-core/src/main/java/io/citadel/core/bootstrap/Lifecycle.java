package io.citadel.core.bootstrap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the Citadel application lifecycle and graceful shutdown.
 *
 * <p>This class is thread-safe. The state machine is driven by {@link LifecycleState} and enforced
 * via atomic compare-and-set on an {@link AtomicReference}.
 *
 * <p>A JVM shutdown hook is registered on the first call to {@link #start()} to handle SIGTERM /
 * SIGINT. The main thread can block on {@link #awaitShutdown()} until termination completes.
 */
public final class Lifecycle {

  private final AtomicReference<LifecycleState> state =
      new AtomicReference<>(LifecycleState.CREATED);
  private final CountDownLatch terminatedLatch = new CountDownLatch(1);
  private final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

  /**
   * Transitions from {@link LifecycleState#CREATED} → {@link LifecycleState#STARTING}.
   *
   * <p>Registers a JVM shutdown hook on the first invocation. Subsequent calls are no-ops.
   *
   * @throws IllegalStateException if the current state is not CREATED
   */
  public void start() {
    if (!state.compareAndSet(LifecycleState.CREATED, LifecycleState.STARTING)) {
      throw new IllegalStateException("Cannot start from state " + state.get());
    }
    registerShutdownHook();
  }

  /**
   * Marks startup as complete: transitions from {@link LifecycleState#STARTING} → {@link
   * LifecycleState#RUNNING}.
   *
   * @throws IllegalStateException if the current state is not STARTING
   */
  public void finishStartup() {
    if (!state.compareAndSet(LifecycleState.STARTING, LifecycleState.RUNNING)) {
      throw new IllegalStateException("Cannot finish startup from state " + state.get());
    }
  }

  /**
   * Initiates graceful shutdown. Idempotent — safely ignored if already shutting down or
   * terminated.
   *
   * <p>If the current state is {@link LifecycleState#RUNNING} or {@link LifecycleState#STARTING},
   * transitions to {@link LifecycleState#SHUTTING_DOWN} and then immediately to {@link
   * LifecycleState#TERMINATED}, counting down the latch so that {@link #awaitShutdown()} unblocks.
   */
  public void shutdown() {
    LifecycleState current = state.get();
    if (current == LifecycleState.TERMINATED || current == LifecycleState.SHUTTING_DOWN) {
      return;
    }
    if ((current == LifecycleState.RUNNING || current == LifecycleState.STARTING)
        && state.compareAndSet(current, LifecycleState.SHUTTING_DOWN)) {
      doTerminate();
    }
  }

  /**
   * Blocks the calling thread until the lifecycle reaches {@link LifecycleState#TERMINATED}.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public void awaitShutdown() throws InterruptedException {
    terminatedLatch.await();
  }

  /** Returns the current lifecycle state. */
  public LifecycleState getState() {
    return state.get();
  }

  private void registerShutdownHook() {
    if (!shutdownHookRegistered.compareAndSet(false, true)) {
      return;
    }
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (state.get() == LifecycleState.RUNNING
                      || state.get() == LifecycleState.STARTING) {
                    shutdown();
                  }
                }));
  }

  private void doTerminate() {
    state.set(LifecycleState.TERMINATED);
    terminatedLatch.countDown();
  }
}
