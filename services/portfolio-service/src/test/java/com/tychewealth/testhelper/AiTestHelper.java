package com.tychewealth.testhelper;

import static com.tychewealth.testdata.AiTestData.TEST_AI_TIMEOUT_SECONDS;
import static com.tychewealth.testdata.AiTestData.TEST_EXTRACTION_TIMEOUT_SECONDS;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_EXTRACTED_CHARACTERS;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_FILE_SIZE_BYTES;
import static com.tychewealth.testdata.AiTestData.TEST_MAX_PDF_PAGES;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.ai.AiQueueBlockingPolicy;
import com.tychewealth.client.AiClient;
import com.tychewealth.service.helper.asset.AssetValidationHelper;
import com.tychewealth.service.helper.asset.ai.AiResponseParser;
import com.tychewealth.service.helper.asset.ai.ImportAssetsAiHelper;
import com.tychewealth.service.helper.asset.ai.support.AiResponseParserSupport;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;

public final class AiTestHelper {

  private AiTestHelper() {}

  public static ThreadPoolExecutor buildAiExecutor(
      AssetValidationHelper validationHelper,
      int maxConcurrency,
      int queueCapacity,
      long queueOfferTimeoutSeconds) {
    Duration aiRequestTimeout =
        Duration.ofSeconds(Math.max(1L, validationHelper.aiTimeoutSeconds()));
    Duration requestedQueueOfferTimeout =
        Duration.ofSeconds(Math.max(1L, queueOfferTimeoutSeconds));
    Duration aiQueueOfferTimeout =
        requestedQueueOfferTimeout.compareTo(aiRequestTimeout) > 0
            ? aiRequestTimeout
            : requestedQueueOfferTimeout;

    return new ThreadPoolExecutor(
        Math.max(1, maxConcurrency),
        Math.max(1, maxConcurrency),
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(Math.max(1, queueCapacity)),
        new AiQueueBlockingPolicy(aiQueueOfferTimeout));
  }

  public static ImportAssetsAiHelper buildImportAssetsAiHelper(
      AiClient aiClient,
      AssetValidationHelper validationHelper,
      RedisTemplate<String, String> redisTemplate,
      ThreadPoolExecutor executor) {
    return new ImportAssetsAiHelper(
        aiClient,
        Duration.ofSeconds(Math.max(1L, validationHelper.aiTimeoutSeconds())),
        validationHelper,
        redisTemplate,
        executor);
  }

  public static AiResponseParser buildAiResponseParser(
      ObjectMapper objectMapper, AiResponseParserSupport parserSupport, int maxDetectedAssets) {
    return new AiResponseParser(
        objectMapper,
        new AssetValidationHelper(
            TEST_MAX_FILE_SIZE_BYTES,
            TEST_MAX_PDF_PAGES,
            TEST_MAX_EXTRACTED_CHARACTERS,
            TEST_EXTRACTION_TIMEOUT_SECONDS,
            TEST_AI_TIMEOUT_SECONDS,
            maxDetectedAssets),
        parserSupport);
  }

  public static void awaitQueuedTask(ThreadPoolExecutor executor, long timeout, TimeUnit unit) {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      if (!executor.getQueue().isEmpty()) {
        return;
      }
      Thread.onSpinWait();
    }
    throw new AssertionError("Timed out waiting for queued ai task");
  }
}
