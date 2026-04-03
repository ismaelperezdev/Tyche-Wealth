package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.LogConstants.AI_PROCESSING_START_MESSAGE;
import static com.tychewealth.constants.LogConstants.AI_PROCESSING_SUCCESS_CONTEXT;
import static com.tychewealth.constants.LogConstants.AI_PROCESSING_SUCCESS_MESSAGE;
import static com.tychewealth.constants.LogConstants.AI_QUEUE_FULL_WAIT_MESSAGE;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.client.AiClient;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.utils.AiUtils;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImportAssetsAiHelper {

  private static final String AI_CACHE_KEY_PREFIX = "asset-import:ai:";
  private static final Duration AI_CACHE_TTL = Duration.ofHours(12);

  private final AiClient aiClient;
  private final AssetValidationHelper assetValidationHelper;
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final ThreadPoolExecutor aiExecutor;

  public ImportAssetsAiHelper(
      AiClient aiClient,
      AssetValidationHelper assetValidationHelper,
      RedisTemplate<String, String> redisTemplate,
      ObjectMapper objectMapper,
      @Value("${app.asset.import.ai.queue.max-concurrency:1}") int maxConcurrency,
      @Value("${app.asset.import.ai.queue.capacity:20}") int queueCapacity) {
    this.aiClient = aiClient;
    this.assetValidationHelper = assetValidationHelper;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.aiExecutor =
        new ThreadPoolExecutor(
            Math.max(1, maxConcurrency),
            Math.max(1, maxConcurrency),
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(Math.max(1, queueCapacity)),
            new BlockingQueuePolicy());
  }

  public String promptFast(String prompt) {
    return execute(prompt, AiModelTypeEnum.FAST);
  }

  public String promptComplex(String prompt) {
    return execute(prompt, AiModelTypeEnum.COMPLEX);
  }

  public List<AssetImportCandidateDto> promptFastAndParse(String prompt) {
    return parseAiAssets(promptFast(prompt));
  }

  private String execute(String prompt, AiModelTypeEnum modelType) {
    String cacheKey = buildCacheKey(prompt, modelType);
    String cachedResponse = redisTemplate.opsForValue().get(cacheKey);
    if (cachedResponse != null && !cachedResponse.isBlank()) {
      return cachedResponse;
    }

    log.info(
        REQUEST_START + AI_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        AI_REQUEST_QUEUED_MESSAGE,
        modelType,
        aiExecutor.getActiveCount(),
        aiExecutor.getQueue().size());

    Future<String> future = aiExecutor.submit(() -> callAi(prompt, modelType));
    try {
      String response = future.get(assetValidationHelper.aiTimeoutSeconds(), TimeUnit.SECONDS);
      log.info(
          REQUEST_SUCCESS + AI_QUEUE_STATUS,
          ASSET,
          IMPORT_ASSETS_ACTION,
          AI_REQUEST_COMPLETED_MESSAGE,
          modelType,
          aiExecutor.getActiveCount(),
          aiExecutor.getQueue().size());
      redisTemplate.opsForValue().set(cacheKey, response, AI_CACHE_TTL);
      return response;
    } catch (InterruptedException ex) {
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
      throw assetValidationHelper.aiTimeoutExceeded(assetValidationHelper.aiTimeoutSeconds());
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

  private String buildCacheKey(String prompt, AiModelTypeEnum modelType) {
    return AI_CACHE_KEY_PREFIX
        + modelType.name()
        + ":"
        + com.tychewealth.utils.Utils.sha256Hex(prompt);
  }

  public List<AssetImportCandidateDto> parseAiAssets(String aiResponse) {
    try {
      JavaType type =
          objectMapper
              .getTypeFactory()
              .constructCollectionType(List.class, AssetImportCandidateDto.class);
      String sanitizedResponse = AiUtils.sanitizeAiResponse(aiResponse);
      List<AssetImportCandidateDto> assets = objectMapper.readValue(sanitizedResponse, type);
      assetValidationHelper.validateDetectedAssetsCount(assets.size());
      return assets;
    } catch (JsonProcessingException ex) {
      throw new AssetImportException(
          ErrorDefinition.ASSET_IMPORT_AI_RESPONSE_INVALID,
          Map.of("error", ex.getOriginalMessage()),
          HttpStatus.BAD_REQUEST);
    }
  }

  private String callAi(String prompt, AiModelTypeEnum modelType) {
    long startTime = System.nanoTime();
    log.info(
        REQUEST_START + AI_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        AI_PROCESSING_START_MESSAGE,
        modelType,
        aiExecutor.getActiveCount(),
        aiExecutor.getQueue().size());
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

  @PreDestroy
  void shutdown() {
    aiExecutor.shutdown();
  }

  private static final class BlockingQueuePolicy implements RejectedExecutionHandler {

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
        executor.getQueue().put(runnable);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RejectedExecutionException("Interrupted while waiting for ai queue", ex);
      }
    }
  }
}
