package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.CommonConstants.EXPECTED;
import static com.tychewealth.constants.CommonConstants.RECEIVED;
import static com.tychewealth.constants.CommonConstants.UNKNOWN_VALUE;
import static com.tychewealth.constants.LogConstants.ASSET;
import static com.tychewealth.constants.LogConstants.FILE_NAME_CONTEXT;
import static com.tychewealth.constants.LogConstants.IMPORT_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.IMPORT_COMPLETED_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_EXTRACTION_IO_FAILURE_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_FAILED_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_INTERRUPTED_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_PROCESSING_START_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_PROCESSING_SUCCESS_CONTEXT;
import static com.tychewealth.constants.LogConstants.IMPORT_PROCESSING_SUCCESS_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_QUEUEING_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_QUEUE_FULL_WAIT_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_QUEUE_STATUS;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.utils.FileDataExtractor;
import com.tychewealth.utils.Utils;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
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
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class ImportAssetsHelper {

  private static final String IMPORT_CACHE_KEY_PREFIX = "asset-import:payload:";
  private static final Duration IMPORT_CACHE_TTL = Duration.ofHours(12);

  private final AssetValidationHelper assetValidationHelper;
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final ThreadPoolExecutor importExecutor;

  public ImportAssetsHelper(
      AssetValidationHelper assetValidationHelper,
      RedisTemplate<String, String> redisTemplate,
      ObjectMapper objectMapper,
      @Value("${app.asset.import.queue.max-concurrency:4}") int maxConcurrency,
      @Value("${app.asset.import.queue.capacity:50}") int queueCapacity) {
    this.assetValidationHelper = assetValidationHelper;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.importExecutor =
        new ThreadPoolExecutor(
            Math.max(1, maxConcurrency),
            Math.max(1, maxConcurrency),
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(Math.max(1, queueCapacity)),
            new BlockingQueuePolicy());
  }

  public AssetImportResponseDto buildImportPayload(MultipartFile file) {
    String cacheKey = buildCacheKey(file);
    AssetImportResponseDto cachedResponse = readCachedResponse(cacheKey);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    String fileName = resolveFileName(file);
    log.info(
        REQUEST_START + IMPORT_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        IMPORT_QUEUEING_MESSAGE,
        fileName,
        importExecutor.getActiveCount(),
        importExecutor.getQueue().size());

    Future<AssetImportResponseDto> future =
        importExecutor.submit(() -> extractCommonPayload(file, fileName));
    try {
      AssetImportResponseDto response =
          future.get(assetValidationHelper.extractionTimeoutSeconds(), TimeUnit.SECONDS);
      log.info(
          REQUEST_SUCCESS + IMPORT_QUEUE_STATUS,
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_COMPLETED_MESSAGE,
          fileName,
          importExecutor.getActiveCount(),
          importExecutor.getQueue().size());
      writeCachedResponse(cacheKey, response);
      return response;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn(
          REQUEST_CONFLICT + FILE_NAME_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_INTERRUPTED_MESSAGE,
          fileName,
          ex);
      throw new IllegalStateException("Asset import processing was interrupted", ex);
    } catch (TimeoutException ex) {
      future.cancel(true);
      throw assetValidationHelper.extractionTimeoutExceeded(
          assetValidationHelper.extractionTimeoutSeconds());
    } catch (ExecutionException ex) {
      log.error(
          REQUEST_CONFLICT + FILE_NAME_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_FAILED_MESSAGE,
          fileName,
          ex.getCause());
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Asset import processing failed", cause);
    }
  }

  private String buildCacheKey(MultipartFile file) {
    try {
      return IMPORT_CACHE_KEY_PREFIX + Utils.sha256Hex(file.getBytes());
    } catch (IOException ex) {
      throw new AssetImportException(
          ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED, Map.of(), HttpStatus.BAD_REQUEST);
    }
  }

  private AssetImportResponseDto readCachedResponse(String cacheKey) {
    String cachedJson = redisTemplate.opsForValue().get(cacheKey);
    if (cachedJson == null || cachedJson.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(cachedJson, AssetImportResponseDto.class);
    } catch (JsonProcessingException ex) {
      redisTemplate.delete(cacheKey);
      return null;
    }
  }

  private void writeCachedResponse(String cacheKey, AssetImportResponseDto response) {
    try {
      redisTemplate
          .opsForValue()
          .set(cacheKey, objectMapper.writeValueAsString(response), IMPORT_CACHE_TTL);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to cache asset import payload", ex);
    }
  }

  private AssetImportResponseDto extractCommonPayload(MultipartFile file, String fileName) {
    long startTime = System.nanoTime();
    log.info(
        REQUEST_START + IMPORT_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        IMPORT_PROCESSING_START_MESSAGE,
        fileName,
        importExecutor.getActiveCount(),
        importExecutor.getQueue().size());
    try (InputStream inputStream = file.getInputStream()) {
      assetValidationHelper.validateExtractionRequest(fileName, inputStream);
      String extractedText = FileDataExtractor.extractText(fileName, inputStream);
      assetValidationHelper.validateExtractedText(extractedText);
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      log.info(
          REQUEST_SUCCESS + IMPORT_PROCESSING_SUCCESS_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_PROCESSING_SUCCESS_MESSAGE,
          fileName,
          elapsedMillis,
          extractedText.length());
      return new AssetImportResponseDto(fileName, extractedText, null, null);
    } catch (IOException ex) {
      log.warn(
          REQUEST_CONFLICT + FILE_NAME_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_EXTRACTION_IO_FAILURE_MESSAGE,
          fileName,
          ex);
      throw new AssetImportException(
          ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED,
          Map.of(EXPECTED, fileName, RECEIVED, fileName),
          HttpStatus.BAD_REQUEST);
    }
  }

  private String resolveFileName(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    return originalFilename == null || originalFilename.isBlank()
        ? UNKNOWN_VALUE
        : originalFilename;
  }

  @PreDestroy
  void shutdown() {
    importExecutor.shutdown();
  }

  private static final class BlockingQueuePolicy implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
      try {
        log.info(
            REQUEST_START + " activeWorkers={} queuedTasks={}",
            ASSET,
            IMPORT_ASSETS_ACTION,
            IMPORT_QUEUE_FULL_WAIT_MESSAGE,
            executor.getActiveCount(),
            executor.getQueue().size());
        executor.getQueue().put(runnable);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RejectedExecutionException(
            "Interrupted while waiting for asset import queue", ex);
      }
    }
  }
}
