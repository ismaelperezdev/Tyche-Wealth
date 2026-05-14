package com.tychewealth.ai;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AiQueueBlockingPolicyTest {

  private static final Runnable TASK = () -> {};
  private static final Runnable EXISTING_TASK = () -> {};

  @Test
  void rejectedExecutionEnqueuesTaskWhenQueueSpaceBecomesAvailableWithinTimeout()
      throws InterruptedException {
    AiQueueBlockingPolicy policy = new AiQueueBlockingPolicy(Duration.ofMillis(200));
    ThreadPoolExecutor executor = buildExecutor();
    executor.getQueue().put(EXISTING_TASK);
    Thread releaseThread =
        new Thread(
            () -> {
              try {
                Thread.sleep(50);
                executor.getQueue().poll();
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
              }
            });

    try {
      releaseThread.start();

      policy.rejectedExecution(TASK, executor);

      assertSame(TASK, executor.getQueue().peek());
    } finally {
      releaseThread.join();
      executor.shutdownNow();
    }
  }

  @Test
  void rejectedExecutionThrowsWhenQueueRemainsFullUntilTimeout() throws InterruptedException {
    AiQueueBlockingPolicy policy = new AiQueueBlockingPolicy(Duration.ofMillis(50));
    ThreadPoolExecutor executor = buildExecutor();
    executor.getQueue().put(EXISTING_TASK);

    try {
      RejectedExecutionException exception =
          assertThrows(
              RejectedExecutionException.class, () -> policy.rejectedExecution(TASK, executor));

      assertTrue(exception.getMessage().contains("Timed out while waiting for ai queue"));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void rejectedExecutionPreservesInterruptStatusWhenInterruptedWhileWaiting()
      throws InterruptedException {
    AiQueueBlockingPolicy policy = new AiQueueBlockingPolicy(Duration.ofMillis(50));
    ThreadPoolExecutor executor = buildExecutor();
    executor.getQueue().put(EXISTING_TASK);

    try {
      Thread.currentThread().interrupt();

      RejectedExecutionException exception =
          assertThrows(
              RejectedExecutionException.class, () -> policy.rejectedExecution(TASK, executor));

      assertTrue(exception.getMessage().contains("Interrupted while waiting for ai queue"));
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
      executor.shutdownNow();
    }
  }

  private ThreadPoolExecutor buildExecutor() {
    return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
  }
}
