package com.tychewealth.service.helper.asset.ai;

import static com.tychewealth.testdata.AiTestData.TEST_AI_QUEUE_FULL_REASON;
import static com.tychewealth.testdata.AiTestData.TEST_AI_TIMEOUT_SECONDS;
import static com.tychewealth.testdata.AiTestData.TEST_AI_TIMEOUT_SECONDS_TIGHT;
import static com.tychewealth.testdata.AiTestData.TEST_AWAIT_TIMEOUT_SECONDS;
import static com.tychewealth.testdata.AiTestData.TEST_EMPTY_AI_RESPONSE;
import static com.tychewealth.testdata.AiTestData.TEST_EXECUTOR_CONCURRENCY;
import static com.tychewealth.testdata.AiTestData.TEST_EXECUTOR_QUEUE_CAPACITY;
import static com.tychewealth.testdata.AiTestData.TEST_EXPECTED_TIMEOUT_UPPER_BOUND_MILLIS;
import static com.tychewealth.testdata.AiTestData.TEST_EXTRACTION_TIMEOUT_SECONDS;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_DETECTED_ASSETS;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_EXTRACTED_CHARACTERS;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_FILE_SIZE_BYTES;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_PDF_PAGES;
import static com.tychewealth.testdata.AiTestData.TEST_PROMPT_1;
import static com.tychewealth.testdata.AiTestData.TEST_PROMPT_2;
import static com.tychewealth.testdata.AiTestData.TEST_PROMPT_3;
import static com.tychewealth.testdata.AiTestData.TEST_QUEUE_OFFER_TIMEOUT_SECONDS;
import static com.tychewealth.testhelper.AiTestHelper.awaitQueuedTask;
import static com.tychewealth.testhelper.AiTestHelper.buildAiExecutor;
import static com.tychewealth.testhelper.AiTestHelper.buildImportAssetsAiHelper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import com.tychewealth.client.AiClient;
import com.tychewealth.dto.ai.AiModelTypeEnum;
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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.server.ResponseStatusException;

class ImportAssetsAiHelperTest {

  private AssetAiValidationHelper assetAiValidationHelper;
  private InMemoryRedisState redisState;
  private RedisTemplate<String, String> redisTemplate;
  private AiClient aiClient;

  @BeforeEach
  void setUp() {
    assetAiValidationHelper =
        new AssetAiValidationHelper(
            TEST_MAX_FILE_SIZE_BYTES,
            TEST_MAX_PDF_PAGES,
            TEST_MAX_EXTRACTED_CHARACTERS,
            TEST_EXTRACTION_TIMEOUT_SECONDS,
            TEST_AI_TIMEOUT_SECONDS,
            TEST_MAX_DETECTED_ASSETS);
    redisState = new InMemoryRedisState();
    redisTemplate = TestRedisSupport.redisTemplate(redisState);
    aiClient = Mockito.mock(AiClient.class);
  }

  @Test
  void promptFastUsesSingleAiTimeoutBudgetAcrossQueueWaitAndExecution()
      throws ExecutionException, InterruptedException {
    assetAiValidationHelper =
        new AssetAiValidationHelper(
            TEST_MAX_FILE_SIZE_BYTES,
            TEST_MAX_PDF_PAGES,
            TEST_MAX_EXTRACTED_CHARACTERS,
            TEST_EXTRACTION_TIMEOUT_SECONDS,
            TEST_AI_TIMEOUT_SECONDS_TIGHT,
            TEST_MAX_DETECTED_ASSETS);
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
              return TEST_EMPTY_AI_RESPONSE;
            })
        .when(aiClient)
        .prompt(anyString(), any());

    ThreadPoolExecutor executor =
        buildAiExecutor(
            assetAiValidationHelper,
            TEST_EXECUTOR_CONCURRENCY,
            TEST_EXECUTOR_QUEUE_CAPACITY,
            TEST_QUEUE_OFFER_TIMEOUT_SECONDS);
    ImportAssetsAiHelper helper =
        buildImportAssetsAiHelper(aiClient, assetAiValidationHelper, redisTemplate, executor);
    ExecutorService requestExecutor = Executors.newFixedThreadPool(2);

    try {
      Future<String> first =
          requestExecutor.submit(() -> helper.prompt(TEST_PROMPT_1, AiModelTypeEnum.FAST));
      assertTrue(firstCallStarted.await(TEST_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
      Future<String> second =
          requestExecutor.submit(() -> helper.prompt(TEST_PROMPT_2, AiModelTypeEnum.FAST));
      releaseFirstCall.countDown();
      assertTrue(secondCallStarted.await(TEST_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

      long startedAt = System.nanoTime();
      AssetImportException exception =
          assertThrows(
              AssetImportException.class, () -> helper.prompt(TEST_PROMPT_3, AiModelTypeEnum.FAST));
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

      assertEquals(ErrorDefinition.AI_PROCESSING_TIMEOUT_EXCEEDED, exception.getErrorDefinition());
      assertTrue(
          elapsedMillis < TEST_EXPECTED_TIMEOUT_UPPER_BOUND_MILLIS,
          "Expected timeout within the configured AI budget");

      first.get();
      assertThrows(ExecutionException.class, second::get);
      assertTrue(invocationCount.get() >= 2);
    } finally {
      releaseFirstCall.countDown();
      releaseSecondCall.countDown();
      releaseThirdCall.countDown();
      requestExecutor.shutdownNow();
      executor.shutdownNow();
    }
  }

  @Test
  void promptFastMapsRejectedSubmissionToRateLimitFailure() {
    AtomicInteger invocationCount = new AtomicInteger();
    CountDownLatch firstCallStarted = new CountDownLatch(1);
    CountDownLatch releaseFirstCall = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              if (invocationCount.incrementAndGet() == 1) {
                firstCallStarted.countDown();
                releaseFirstCall.await();
              }
              return TEST_EMPTY_AI_RESPONSE;
            })
        .when(aiClient)
        .prompt(anyString(), any());

    ThreadPoolExecutor executor =
        buildAiExecutor(
            assetAiValidationHelper,
            TEST_EXECUTOR_CONCURRENCY,
            TEST_EXECUTOR_QUEUE_CAPACITY,
            TEST_QUEUE_OFFER_TIMEOUT_SECONDS);
    ImportAssetsAiHelper helper =
        buildImportAssetsAiHelper(aiClient, assetAiValidationHelper, redisTemplate, executor);

    try {
      CompletableFuture<String> first =
          CompletableFuture.supplyAsync(() -> helper.prompt(TEST_PROMPT_1, AiModelTypeEnum.FAST));
      assertTrue(firstCallStarted.await(TEST_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
      CompletableFuture<String> second =
          CompletableFuture.supplyAsync(() -> helper.prompt(TEST_PROMPT_2, AiModelTypeEnum.FAST));
      awaitQueuedTask(executor, TEST_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> helper.prompt(TEST_PROMPT_3, AiModelTypeEnum.FAST));

      assertEquals(429, exception.getStatusCode().value());
      assertEquals(TEST_AI_QUEUE_FULL_REASON, exception.getReason());

      releaseFirstCall.countDown();
      first.get();
      second.get();
      assertEquals(2, invocationCount.get());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    } catch (ExecutionException ex) {
      throw new IllegalStateException(ex);
    } finally {
      releaseFirstCall.countDown();
      executor.shutdownNow();
    }
  }
}
