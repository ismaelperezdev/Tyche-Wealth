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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

class ImportAssetsAiHelperTest {

  @Test
  void promptFastUsesSingleAiTimeoutBudgetAcrossQueueWaitAndExecution()
      throws ExecutionException, InterruptedException {
    AssetValidationHelper validationHelper = new AssetValidationHelper(1024L, 10, 5000, 5, 1, 10);
    InMemoryRedisState redisState = new InMemoryRedisState();
    RedisTemplate<String, String> redisTemplate = TestRedisSupport.redisTemplate(redisState);
    AiClient aiClient = Mockito.mock(AiClient.class);
    AtomicInteger invocationCount = new AtomicInteger();

    doAnswer(
            invocation -> {
              invocationCount.incrementAndGet();
              Thread.sleep(400);
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

      long startedAt = System.nanoTime();
      AssetImportException exception =
          assertThrows(AssetImportException.class, () -> helper.promptFast("p3"));
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

      assertEquals(ErrorDefinition.AI_PROCESSING_TIMEOUT_EXCEEDED, exception.getErrorDefinition());
      assertTrue(elapsedMillis < 1500, "Expected timeout within the configured AI budget");

      first.get();
      second.get();
      assertEquals(3, invocationCount.get());
    } finally {
      helper.shutdown();
    }
  }
}
