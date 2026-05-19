package com.tychewealth.service.helper.asset.ai;

import static com.tychewealth.constants.LogConstants.AI_PROCESSING_START_MESSAGE;
import static com.tychewealth.constants.LogConstants.AI_PROCESSING_SUCCESS_CONTEXT;
import static com.tychewealth.constants.LogConstants.AI_PROCESSING_SUCCESS_MESSAGE;
import static com.tychewealth.constants.LogConstants.AI_QUEUE_STATUS;
import static com.tychewealth.constants.LogConstants.AI_REQUEST_COMPLETED_MESSAGE;
import static com.tychewealth.constants.LogConstants.AI_REQUEST_FAILED_MESSAGE;
import static com.tychewealth.constants.LogConstants.AI_REQUEST_INTERRUPTED_MESSAGE;
import static com.tychewealth.constants.LogConstants.AI_REQUEST_QUEUED_MESSAGE;
import static com.tychewealth.constants.LogConstants.ASSET;
import static com.tychewealth.constants.LogConstants.IMPORT_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.MODEL_TYPE_CONTEXT;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.utils.Utils.rateLimited;
import static com.tychewealth.utils.Utils.sha256Hex;

import com.tychewealth.client.AiClient;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImportAssetsAiHelper {

  private static final String AI_CACHE_KEY_PREFIX = "asset-import:ai:";
  private static final Duration AI_CACHE_TTL = Duration.ofHours(12);

  private final Duration aiRequestTimeout;
  private final AiClient aiClient;
  private final AssetAiValidationHelper assetAiValidationHelper;
  private final RedisTemplate<String, String> redisTemplate;
  private final ThreadPoolExecutor aiExecutor;

  public ImportAssetsAiHelper(
      AiClient aiClient,
      Duration assetImportAiRequestTimeout,
      AssetAiValidationHelper assetAiValidationHelper,
      RedisTemplate<String, String> redisTemplate,
      ThreadPoolExecutor assetImportAiExecutor) {
    this.aiClient = aiClient;
    this.aiRequestTimeout = assetImportAiRequestTimeout;
    this.assetAiValidationHelper = assetAiValidationHelper;
    this.redisTemplate = redisTemplate;
    this.aiExecutor = assetImportAiExecutor;
  }

  public String prompt(String prompt, AiModelTypeEnum modelType) {
    String cacheKey = AI_CACHE_KEY_PREFIX + modelType.name() + ":" + sha256Hex(prompt);
    String cachedResponse = readCachedResponse(cacheKey, modelType);

    if (cachedResponse != null && !cachedResponse.isBlank()) {
      return cachedResponse;
    }

    String response = executePrompt(prompt, modelType);
    writeCachedResponse(cacheKey, modelType, response);

    return response;
  }

  private String readCachedResponse(String cacheKey, AiModelTypeEnum modelType) {
    try {
      return redisTemplate.opsForValue().get(cacheKey);
    } catch (RuntimeException ex) {
      log.error(
          REQUEST_CONFLICT + " cacheKey={} " + MODEL_TYPE_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          "asset import ai cache read failed",
          cacheKey,
          modelType,
          ex);
      return null;
    }
  }

  private String executePrompt(String prompt, AiModelTypeEnum modelType) {
    long deadlineNanos = System.nanoTime() + aiRequestTimeout.toNanos();
    Future<String> future;
    long remainingTimeoutNanos;
    String response;

    log.info(
        REQUEST_START + AI_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        AI_REQUEST_QUEUED_MESSAGE,
        modelType,
        aiExecutor.getActiveCount(),
        aiExecutor.getQueue().size());

    try {
      future = aiExecutor.submit(() -> callAi(prompt, modelType));
    } catch (RejectedExecutionException ex) {
      throw rateLimited("AI queue is full");
    }

    try {
      remainingTimeoutNanos = deadlineNanos - System.nanoTime();
      if (remainingTimeoutNanos <= 0) {
        future.cancel(true);
        throw assetAiValidationHelper.aiTimeoutExceeded(aiRequestTimeout.toSeconds());
      }

      response = future.get(remainingTimeoutNanos, TimeUnit.NANOSECONDS);
      log.info(
          REQUEST_SUCCESS + AI_QUEUE_STATUS,
          ASSET,
          IMPORT_ASSETS_ACTION,
          AI_REQUEST_COMPLETED_MESSAGE,
          modelType,
          aiExecutor.getActiveCount(),
          aiExecutor.getQueue().size());

      return response;

    } catch (InterruptedException ex) {

      future.cancel(true);
      Thread.currentThread().interrupt();

      log.warn(
          REQUEST_CONFLICT + MODEL_TYPE_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          AI_REQUEST_INTERRUPTED_MESSAGE,
          modelType,
          ex);

      throw new IllegalStateException("AI processing was interrupted", ex);

    } catch (TimeoutException ex) {
      future.cancel(true);
      throw assetAiValidationHelper.aiTimeoutExceeded(aiRequestTimeout.toSeconds());

    } catch (ExecutionException ex) {
      log.error(
          REQUEST_CONFLICT + MODEL_TYPE_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          AI_REQUEST_FAILED_MESSAGE,
          modelType,
          ex.getCause());

      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }

      throw new IllegalStateException("AI processing failed", cause);
    }
  }

  private void writeCachedResponse(String cacheKey, AiModelTypeEnum modelType, String response) {
    try {
      redisTemplate.opsForValue().set(cacheKey, response, AI_CACHE_TTL);
    } catch (RuntimeException ex) {
      log.error(
          REQUEST_CONFLICT + " cacheKey={} " + MODEL_TYPE_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          "asset import ai cache write failed",
          cacheKey,
          modelType,
          ex);
    }
  }

  private String callAi(String prompt, AiModelTypeEnum modelType) {

    log.info(
        REQUEST_START + AI_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        AI_PROCESSING_START_MESSAGE,
        modelType,
        aiExecutor.getActiveCount(),
        aiExecutor.getQueue().size());

    long startTime = System.nanoTime();
    String response = aiClient.prompt(prompt, modelType);
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

    log.info(
        REQUEST_SUCCESS + AI_PROCESSING_SUCCESS_CONTEXT,
        ASSET,
        IMPORT_ASSETS_ACTION,
        AI_PROCESSING_SUCCESS_MESSAGE,
        modelType,
        elapsedMillis,
        response.length());

    return response;
  }
}
