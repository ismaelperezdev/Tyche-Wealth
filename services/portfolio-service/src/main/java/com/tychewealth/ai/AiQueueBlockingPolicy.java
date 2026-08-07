package com.tychewealth.ai;

import static com.tychewealth.constants.LogConstants.AI_QUEUE_FULL_WAIT_MESSAGE;
import static com.tychewealth.constants.LogConstants.ASSET;
import static com.tychewealth.constants.LogConstants.IMPORT_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.REQUEST_START;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Applies bounded backpressure when the asset-import AI executor cannot accept a new task.
 *
 * <p>Waits for the configured duration to enqueue the task, logs queue pressure, and rejects the
 * submission when capacity is not released in time. Interrupted callers preserve the interrupt flag
 * and receive a RejectedExecutionException.
 */
@Slf4j
public final class AiQueueBlockingPolicy implements RejectedExecutionHandler {

  private final Duration aiQueueOfferTimeout;

  public AiQueueBlockingPolicy(Duration aiQueueOfferTimeout) {
    this.aiQueueOfferTimeout = aiQueueOfferTimeout;
  }

  @Override
  public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
    try {
      log.info(
          REQUEST_START + " activeWorkers={} queuedTasks={}",
          ASSET,
          IMPORT_ASSETS_ACTION,
          AI_QUEUE_FULL_WAIT_MESSAGE,
          executor.getActiveCount(),
          executor.getQueue().size());
      boolean enqueued =
          executor
              .getQueue()
              .offer(runnable, aiQueueOfferTimeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!enqueued) {
        throw new RejectedExecutionException(
            "Timed out while waiting for ai queue after "
                + aiQueueOfferTimeout.toSeconds()
                + " seconds");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RejectedExecutionException("Interrupted while waiting for ai queue", ex);
    }
  }
}
