package com.tychewealth.testhelper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ConcurrentTestHelper {

  private ConcurrentTestHelper() {}

  @SafeVarargs
  public static <T> List<T> runConcurrently(Callable<T>... tasks) throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(tasks.length);
    CountDownLatch ready = new CountDownLatch(tasks.length);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<T>> futures = new ArrayList<>();
      for (Callable<T> task : tasks) {
        futures.add(
            executorService.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new TimeoutException("Timed out waiting to start concurrent tasks");
                  }
                  return task.call();
                }));
      }

      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();

      List<T> results = new ArrayList<>();
      for (Future<T> future : futures) {
        results.add(getFutureValue(future));
      }
      return results;
    } finally {
      executorService.shutdownNow();
    }
  }

  private static <T> T getFutureValue(Future<T> future) throws Exception {
    try {
      return future.get(10, TimeUnit.SECONDS);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      throw ex;
    }
  }
}
