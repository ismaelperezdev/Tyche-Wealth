package com.tychewealth.service.helper.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.client.AiClient;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.testhelper.TestRedisSupport;
import com.tychewealth.testhelper.TestRedisSupport.InMemoryRedisState;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.server.ResponseStatusException;

class ImportAssetsAiHelperTest {

  @Test
  void promptFastUsesSingleAiTimeoutBudgetAcrossQueueWaitAndExecution()
      throws ExecutionException, InterruptedException {
    AssetValidationHelper validationHelper = new AssetValidationHelper(1024L, 10, 5000, 5, 1, 10);
    InMemoryRedisState redisState = new InMemoryRedisState();
    RedisTemplate<String, String> redisTemplate = TestRedisSupport.redisTemplate(redisState);
    AiClient aiClient = Mockito.mock(AiClient.class);
    AtomicInteger invocationCount = new AtomicInteger();
    CountDownLatch firstCallStarted = new CountDownLatch(1);
    CountDownLatch releaseFirstCall = new CountDownLatch(1);
    CountDownLatch secondCallStarted = new CountDownLatch(1);
    CountDownLatch releaseSecondCall = new CountDownLatch(1);
    CountDownLatch thirdCallStarted = new CountDownLatch(1);
    CountDownLatch releaseThirdCall = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              int currentCall = invocationCount.incrementAndGet();
              if (currentCall == 1) {
                firstCallStarted.countDown();
                releaseFirstCall.await();
              } else if (currentCall == 2) {
                secondCallStarted.countDown();
                releaseSecondCall.await();
              } else if (currentCall == 3) {
                thirdCallStarted.countDown();
                releaseThirdCall.await();
              }
              return "[]";
            })
        .when(aiClient)
        .prompt(anyString(), any());

    ImportAssetsAiHelper helper =
        new ImportAssetsAiHelper(
            aiClient, validationHelper, redisTemplate, new ObjectMapper(), 1, 1, 1);
    ExecutorService requestExecutor = Executors.newFixedThreadPool(2);

    try {
      Future<String> first = requestExecutor.submit(() -> helper.promptFast("p1"));
      assertTrue(firstCallStarted.await(1, TimeUnit.SECONDS));
      Future<String> second = requestExecutor.submit(() -> helper.promptFast("p2"));
      releaseFirstCall.countDown();
      assertTrue(secondCallStarted.await(1, TimeUnit.SECONDS));

      long startedAt = System.nanoTime();
      AssetImportException exception =
          assertThrows(AssetImportException.class, () -> helper.promptFast("p3"));
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

      assertEquals(ErrorDefinition.AI_PROCESSING_TIMEOUT_EXCEEDED, exception.getErrorDefinition());
      assertTrue(elapsedMillis < 1500, "Expected timeout within the configured AI budget");

      first.get();
      assertThrows(ExecutionException.class, second::get);
      assertTrue(invocationCount.get() >= 2);
    } finally {
      releaseFirstCall.countDown();
      releaseSecondCall.countDown();
      releaseThirdCall.countDown();
      requestExecutor.shutdownNow();
      helper.shutdown();
    }
  }

  @Test
  void promptFastMapsRejectedSubmissionToRateLimitFailure() {
    AssetValidationHelper validationHelper = new AssetValidationHelper(1024L, 10, 5000, 5, 3, 10);
    InMemoryRedisState redisState = new InMemoryRedisState();
    RedisTemplate<String, String> redisTemplate = TestRedisSupport.redisTemplate(redisState);
    AiClient aiClient = Mockito.mock(AiClient.class);
    AtomicInteger invocationCount = new AtomicInteger();

    doAnswer(
            invocation -> {
              invocationCount.incrementAndGet();
              Thread.sleep(1500);
              return "[]";
            })
        .when(aiClient)
        .prompt(anyString(), any());

    ImportAssetsAiHelper helper =
        new ImportAssetsAiHelper(
            aiClient, validationHelper, redisTemplate, new ObjectMapper(), 1, 1, 1);

    try {
      CompletableFuture<String> first =
          CompletableFuture.supplyAsync(() -> helper.promptFast("p1"));
      Thread.sleep(100);
      CompletableFuture<String> second =
          CompletableFuture.supplyAsync(() -> helper.promptFast("p2"));
      Thread.sleep(100);

      ResponseStatusException exception =
          assertThrows(ResponseStatusException.class, () -> helper.promptFast("p3"));

      assertEquals(429, exception.getStatusCode().value());
      assertEquals("AI queue is full", exception.getReason());

      first.get();
      second.get();
      assertEquals(2, invocationCount.get());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    } catch (ExecutionException ex) {
      throw new IllegalStateException(ex);
    } finally {
      helper.shutdown();
    }
  }

  @Test
  void parseAiAssetsTreatsJsonNullAsEmptyList() {
    AssetValidationHelper validationHelper = new AssetValidationHelper(1024L, 10, 5000, 5, 3, 10);
    InMemoryRedisState redisState = new InMemoryRedisState();
    RedisTemplate<String, String> redisTemplate = TestRedisSupport.redisTemplate(redisState);
    AiClient aiClient = Mockito.mock(AiClient.class);
    ImportAssetsAiHelper helper =
        new ImportAssetsAiHelper(
            aiClient, validationHelper, redisTemplate, new ObjectMapper(), 1, 1, 1);

    assertTrue(helper.parseAiAssets("null").isEmpty());

    helper.shutdown();
  }

  @Test
  void parseAiAssetsIgnoresWhitespaceOnlyTextFields() {
    AssetValidationHelper validationHelper = new AssetValidationHelper(1024L, 10, 5000, 5, 3, 10);
    InMemoryRedisState redisState = new InMemoryRedisState();
    RedisTemplate<String, String> redisTemplate = TestRedisSupport.redisTemplate(redisState);
    AiClient aiClient = Mockito.mock(AiClient.class);
    ImportAssetsAiHelper helper =
        new ImportAssetsAiHelper(
            aiClient, validationHelper, redisTemplate, new ObjectMapper(), 1, 1, 1);

    assertTrue(helper.parseAiAssets("[{\"name\":\"   \",\"symbol\":\"\\t\"}]").isEmpty());

    helper.shutdown();
  }
}
