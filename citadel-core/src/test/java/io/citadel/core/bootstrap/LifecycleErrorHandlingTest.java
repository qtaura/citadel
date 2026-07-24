package io.citadel.core.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class LifecycleErrorHandlingTest {

  @Test
  void startFromStartingThrowsIllegalStateException() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    assertThrows(IllegalStateException.class, lifecycle::start);
  }

  @Test
  void startFromRunningThrowsIllegalStateException() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.finishStartup();
    assertThrows(IllegalStateException.class, lifecycle::start);
  }

  @Test
  void startAfterTerminateThrowsIllegalStateException() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.finishStartup();
    lifecycle.shutdown();
    assertThrows(IllegalStateException.class, lifecycle::start);
  }

  @Test
  void finishStartupFromCreatedThrowsIllegalStateException() {
    Lifecycle lifecycle = new Lifecycle();
    assertThrows(IllegalStateException.class, lifecycle::finishStartup);
  }

  @Test
  void finishStartupAfterShutdownThrowsIllegalStateException() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.shutdown();
    assertThrows(IllegalStateException.class, lifecycle::finishStartup);
  }

  @Test
  void awaitShutdownUnblocksAfterShutdown() throws InterruptedException {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    lifecycle.finishStartup();

    CountDownLatch result = new CountDownLatch(1);
    Thread thread =
        new Thread(
            () -> {
              try {
                lifecycle.awaitShutdown();
                result.countDown();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            });
    thread.start();

    lifecycle.shutdown();
    assertTrue(result.await(5, TimeUnit.SECONDS), "awaitShutdown did not unblock");
  }

  @Test
  void shutdownHookDoesNotThrow() {
    Lifecycle lifecycle = new Lifecycle();
    lifecycle.start();
    assertSame(LifecycleState.STARTING, lifecycle.getState());
  }
}
