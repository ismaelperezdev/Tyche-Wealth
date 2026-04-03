package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.CommonConstants.EXPECTED;
import static com.tychewealth.constants.CommonConstants.RECEIVED;
import static com.tychewealth.constants.LogConstants.ASSET;
import static com.tychewealth.constants.LogConstants.FILE_NAME_CONTEXT;
import static com.tychewealth.constants.LogConstants.IMPORT_ASSETS_ACTION;
import static com.tychewealth.constants.LogConstants.IMPORT_COMPLETED_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_EXTRACTION_IO_FAILURE_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_FAILED_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_INFLIGHT_ACQUIRED_AND_QUEUED_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_INFLIGHT_RELEASED_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_INFLIGHT_WAIT_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_INTERRUPTED_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_PROCESSING_START_MESSAGE;
import static com.tychewealth.constants.LogConstants.IMPORT_PROCESSING_SUCCESS_CONTEXT;
import static com.tychewealth.constants.LogConstants.IMPORT_PROCESSING_SUCCESS_MESSAGE;
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
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
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
  private static final String IMPORT_INFLIGHT_KEY_PREFIX = "asset-import:payload:inflight:";
  private static final String CACHE_KEY_CONTEXT = " cacheKey={}";
  private static final Duration IMPORT_CACHE_TTL = Duration.ofHours(12);
  private static final Duration IMPORT_QUEUE_OFFER_TIMEOUT = Duration.ofMillis(100);
  private static final Duration IMPORT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

  private final AssetValidationHelper assetValidationHelper;
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final ThreadPoolExecutor importExecutor;
  private final Duration inflightLockTtl;
  private final Duration inflightWaitTimeout;
  private final Duration inflightPollInterval;

  public ImportAssetsHelper(
      AssetValidationHelper assetValidationHelper,
      RedisTemplate<String, String> redisTemplate,
      ObjectMapper objectMapper,
      @Value("${app.asset.import.queue.max-concurrency:4}") int maxConcurrency,
      @Value("${app.asset.import.queue.capacity:50}") int queueCapacity,
      @Value("${app.asset.import.inflight.lock-ttl-seconds:60}") long inflightLockTtlSeconds,
      @Value("${app.asset.import.inflight.wait-timeout-seconds:30}")
          long inflightWaitTimeoutSeconds,
      @Value("${app.asset.import.inflight.poll-interval-millis:200}")
          long inflightPollIntervalMillis) {
    this.assetValidationHelper = assetValidationHelper;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.inflightLockTtl = Duration.ofSeconds(Math.max(1L, inflightLockTtlSeconds));
    this.inflightWaitTimeout = Duration.ofSeconds(Math.max(1L, inflightWaitTimeoutSeconds));
    this.inflightPollInterval = Duration.ofMillis(Math.max(50L, inflightPollIntervalMillis));
    this.importExecutor =
        new ThreadPoolExecutor(
            Math.max(1, maxConcurrency),
            Math.max(1, maxConcurrency),
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(Math.max(1, queueCapacity)));
  }

  public AssetImportResponseDto buildImportPayload(MultipartFile file) {
    String fileName = Utils.resolveFileName(file);
    String cacheKey = buildCacheKey(file, fileName);
    String inflightKey = buildInflightKey(cacheKey);
    while (true) {
      AssetImportResponseDto cachedResponse = readCachedResponse(cacheKey);
      if (cachedResponse != null) {
        return cachedResponse;
      }

      if (tryAcquireInflightLock(inflightKey)) {
        return processImport(file, fileName, cacheKey, inflightKey);
      }

      waitForInflightResult(cacheKey, inflightKey);
    }
  }

  private String buildCacheKey(MultipartFile file, String fileName) {
    try {
      return IMPORT_CACHE_KEY_PREFIX
          + Utils.sha256Hex(fileName.toLowerCase() + ":" + Utils.sha256Hex(file.getBytes()));
    } catch (IOException ex) {
      throw new AssetImportException(
          ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED, Map.of(), HttpStatus.BAD_REQUEST);
    }
  }

  private String buildInflightKey(String cacheKey) {
    return IMPORT_INFLIGHT_KEY_PREFIX + cacheKey.substring(IMPORT_CACHE_KEY_PREFIX.length());
  }

  private boolean tryAcquireInflightLock(String inflightKey) {
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(inflightKey, "1", inflightLockTtl);
    return Boolean.TRUE.equals(acquired);
  }

  private AssetImportResponseDto processImport(
      MultipartFile file, String fileName, String cacheKey, String inflightKey) {
    log.info(
        REQUEST_START + IMPORT_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        IMPORT_INFLIGHT_ACQUIRED_AND_QUEUED_MESSAGE,
        fileName,
        importExecutor.getActiveCount(),
        importExecutor.getQueue().size());

    Future<AssetImportResponseDto> future = submitImportTask(file, fileName);
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
    } finally {
      try {
        redisTemplate.delete(inflightKey);
      } catch (RuntimeException ex) {
        log.warn(
            REQUEST_CONFLICT + FILE_NAME_CONTEXT + " inflightKey={}",
            ASSET,
            IMPORT_ASSETS_ACTION,
            "asset import inflight lock release failed",
            fileName,
            inflightKey,
            ex);
      }
      log.info(
          REQUEST_SUCCESS + FILE_NAME_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_INFLIGHT_RELEASED_MESSAGE,
          fileName);
    }
  }

  private void waitForInflightResult(String cacheKey, String inflightKey) {
    long deadline = System.nanoTime() + inflightWaitTimeout.toNanos();
    log.info(REQUEST_START, ASSET, IMPORT_ASSETS_ACTION, IMPORT_INFLIGHT_WAIT_MESSAGE);

    while (System.nanoTime() < deadline) {
      AssetImportResponseDto cachedResponse = readCachedResponse(cacheKey);
      if (cachedResponse != null) {
        return;
      }

      Boolean inflightExists = redisTemplate.hasKey(inflightKey);
      if (!Boolean.TRUE.equals(inflightExists)) {
        return;
      }

      try {
        Thread.sleep(inflightPollInterval.toMillis());
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for in-flight asset import", ex);
      }
    }

    throw assetValidationHelper.extractionTimeoutExceeded(inflightWaitTimeout.toSeconds());
  }

  private AssetImportResponseDto readCachedResponse(String cacheKey) {
    try {
      String cachedJson = redisTemplate.opsForValue().get(cacheKey);
      if (cachedJson == null || cachedJson.isBlank()) {
        return null;
      }

      return objectMapper.readValue(cachedJson, AssetImportResponseDto.class);
    } catch (JsonProcessingException ex) {
      try {
        redisTemplate.delete(cacheKey);
      } catch (RuntimeException deleteEx) {
        log.error(
            REQUEST_CONFLICT + CACHE_KEY_CONTEXT,
            ASSET,
            IMPORT_ASSETS_ACTION,
            "asset import cache delete failed",
            cacheKey,
            deleteEx);
      }
      return null;
    } catch (RuntimeException ex) {
      log.error(
          REQUEST_CONFLICT + CACHE_KEY_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          "asset import cache read failed",
          cacheKey,
          ex);
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
    } catch (RuntimeException ex) {
      log.error(
          REQUEST_CONFLICT + CACHE_KEY_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          "asset import cache write failed",
          cacheKey,
          ex);
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

  private Future<AssetImportResponseDto> submitImportTask(MultipartFile file, String fileName) {
    FutureTask<AssetImportResponseDto> task =
        new FutureTask<>(() -> extractCommonPayload(file, fileName));

    if (importExecutor.isShutdown()) {
      throw new IllegalStateException("Asset import queue is unavailable");
    }

    if (importExecutor.getQueue().offer(task)) {
      ensureWorkerScheduled();
      return task;
    }

    return submitDirectOrReject(task);
  }

  private Future<AssetImportResponseDto> submitDirectOrReject(
      FutureTask<AssetImportResponseDto> task) {
    try {
      importExecutor.execute(task);
      return task;
    } catch (RejectedExecutionException ex) {
      awaitQueueCapacity(task);
      return task;
    }
  }

  private void awaitQueueCapacity(FutureTask<AssetImportResponseDto> task) {
    try {
      log.info(
          REQUEST_START + " {} activeWorkers={} queuedTasks={}",
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_QUEUE_FULL_WAIT_MESSAGE,
          importExecutor.getActiveCount(),
          importExecutor.getQueue().size());
      boolean enqueued =
          importExecutor
              .getQueue()
              .offer(task, IMPORT_QUEUE_OFFER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      if (!enqueued) {
        throw Utils.rateLimited("Asset import queue is full");
      }
      ensureWorkerScheduled();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for asset import queue", ex);
    }
  }

  private void ensureWorkerScheduled() {
    if (importExecutor.getPoolSize() == 0
        || importExecutor.getActiveCount() < importExecutor.getCorePoolSize()) {
      try {
        importExecutor.prestartCoreThread();
      } catch (RuntimeException ignored) {
        // The queued task will still be picked up by an existing worker if one is available.
      }
    }
  }

  @PreDestroy
  void shutdown() {
    importExecutor.shutdown();
    try {
      if (!importExecutor.awaitTermination(IMPORT_SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
        log.warn(
            REQUEST_CONFLICT + IMPORT_QUEUE_STATUS,
            ASSET,
            IMPORT_ASSETS_ACTION,
            "asset import executor did not terminate in time; forcing shutdown",
            importExecutor.getActiveCount(),
            importExecutor.getQueue().size());
        importExecutor.shutdownNow();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn(
          REQUEST_CONFLICT + IMPORT_QUEUE_STATUS,
          ASSET,
          IMPORT_ASSETS_ACTION,
          "asset import executor shutdown was interrupted; forcing shutdown",
          importExecutor.getActiveCount(),
          importExecutor.getQueue().size(),
          ex);
      importExecutor.shutdownNow();
    }
  }
}
