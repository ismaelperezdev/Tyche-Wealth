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
import static com.tychewealth.constants.RedisConstants.ASSET_IMPORT_INFLIGHT_KEY_PREFIX;
import static com.tychewealth.constants.RedisConstants.ASSET_IMPORT_PAYLOAD_CACHE_KEY_PREFIX;
import static com.tychewealth.constants.RedisConstants.ASSET_IMPORT_RESULT_KEY_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.asset.AssetPersistRedisDto;
import com.tychewealth.dto.asset.request.AssetImportPayloadDto;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.helper.asset.ai.AssetAiValidationHelper;
import com.tychewealth.utils.FileDataExtractor;
import com.tychewealth.utils.Utils;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

/**
 * Coordinates file extraction and temporary persistence for asset imports.
 *
 * <p>Uses a bounded executor to process files, Redis to cache extracted payloads and coordinate
 * in-flight requests, and configurable timeouts and TTLs to control import resource usage and
 * result availability.
 */
@Slf4j
@Component
public class ImportAssetsHelper {

  private static final String CACHE_KEY_CONTEXT = " cacheKey={}";
  private static final Duration IMPORT_CACHE_TTL = Duration.ofHours(12);
  private static final Duration IMPORT_RESULT_TTL = Duration.ofHours(12);
  private static final Duration IMPORT_QUEUE_OFFER_TIMEOUT = Duration.ofMillis(100);
  private static final Duration IMPORT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

  private final AssetAiValidationHelper assetAiValidationHelper;
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final ThreadPoolExecutor importExecutor;
  private final ConcurrentHashMap<MultipartFile, CompletableFuture<AssetImportPayloadDto>>
      localImports = new ConcurrentHashMap<>();
  private final Duration inflightLockTtl;
  private final Duration inflightWaitTimeout;
  private final Duration inflightPollInterval;

  public ImportAssetsHelper(
      AssetAiValidationHelper assetAiValidationHelper,
      RedisTemplate<String, String> redisTemplate,
      ObjectMapper objectMapper,
      @Value("${app.asset.import.queue.max-concurrency:4}") int maxConcurrency,
      @Value("${app.asset.import.queue.capacity:50}") int queueCapacity,
      @Value("${app.asset.import.inflight.lock-ttl-seconds:60}") long inflightLockTtlSeconds,
      @Value("${app.asset.import.inflight.wait-timeout-seconds:30}")
          long inflightWaitTimeoutSeconds,
      @Value("${app.asset.import.inflight.poll-interval-millis:200}")
          long inflightPollIntervalMillis) {
    this.assetAiValidationHelper = assetAiValidationHelper;
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

  public AssetImportPayloadDto prepareImport(MultipartFile file) {
    CompletableFuture<AssetImportPayloadDto> localImport = new CompletableFuture<>();
    CompletableFuture<AssetImportPayloadDto> existingImport =
        localImports.putIfAbsent(file, localImport);
    if (existingImport != null) {
      return awaitLocalImport(existingImport);
    }

    try {
      AssetImportPayloadDto response = prepareImportInternal(file);
      localImport.complete(response);
      return response;
    } catch (RuntimeException ex) {
      localImport.completeExceptionally(ex);
      throw ex;
    } finally {
      localImports.remove(file, localImport);
    }
  }

  private AssetImportPayloadDto prepareImportInternal(MultipartFile file) {
    String fileName = Utils.resolveFileName(file);
    byte[] fileBytes;

    try {
      fileBytes = file.getBytes();
    } catch (IOException ex) {
      throw new AssetImportException(
          ErrorDefinition.ASSET_IMPORT_EXTRACTION_FAILED, Map.of(), HttpStatus.BAD_REQUEST);
    }

    String importId =
        Utils.sha256Hex(fileName.toLowerCase(Locale.ROOT) + ":" + Utils.sha256Hex(fileBytes));
    String cacheKey = ASSET_IMPORT_PAYLOAD_CACHE_KEY_PREFIX + importId;
    String inflightKey = ASSET_IMPORT_INFLIGHT_KEY_PREFIX + importId;
    while (true) {
      AssetImportPayloadDto cachedResponse = readCachedResponse(cacheKey, importId);
      if (cachedResponse != null) {
        return cachedResponse;
      }

      Boolean acquired = redisTemplate.opsForValue().setIfAbsent(inflightKey, "1", inflightLockTtl);
      if (Boolean.TRUE.equals(acquired)) {
        return processImport(file, fileName, fileBytes, importId, cacheKey, inflightKey);
      }

      waitForInflightResult(cacheKey, inflightKey, importId);
    }
  }

  private AssetImportPayloadDto awaitLocalImport(
      CompletableFuture<AssetImportPayloadDto> existingImport) {
    try {
      return existingImport.get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for asset import", ex);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Asset import processing failed", cause);
    }
  }

  private AssetImportPayloadDto processImport(
      MultipartFile file,
      String fileName,
      byte[] fileBytes,
      String importId,
      String cacheKey,
      String inflightKey) {
    log.info(
        REQUEST_START + IMPORT_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        IMPORT_INFLIGHT_ACQUIRED_AND_QUEUED_MESSAGE,
        fileName,
        importExecutor.getActiveCount(),
        importExecutor.getQueue().size());

    Future<AssetImportPayloadDto> future = null;
    try {
      future = submitImportTask(file, fileBytes, fileName, importId);
      AssetImportPayloadDto response =
          future.get(assetAiValidationHelper.extractionTimeoutSeconds(), TimeUnit.SECONDS);
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
      throw assetAiValidationHelper.extractionTimeoutExceeded(
          assetAiValidationHelper.extractionTimeoutSeconds());
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
      releaseInflightLock(fileName, inflightKey);
    }
  }

  private void releaseInflightLock(String fileName, String inflightKey) {
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

  private void waitForInflightResult(String cacheKey, String inflightKey, String importId) {
    long deadline = System.nanoTime() + inflightWaitTimeout.toNanos();
    log.info(REQUEST_START, ASSET, IMPORT_ASSETS_ACTION, IMPORT_INFLIGHT_WAIT_MESSAGE);

    while (System.nanoTime() < deadline) {
      AssetImportPayloadDto cachedResponse = readCachedResponse(cacheKey, importId);
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

    throw assetAiValidationHelper.extractionTimeoutExceeded(inflightWaitTimeout.toSeconds());
  }

  private AssetImportPayloadDto readCachedResponse(String cacheKey, String importId) {
    try {
      String cachedJson = redisTemplate.opsForValue().get(cacheKey);
      if (cachedJson == null || cachedJson.isBlank()) {
        return null;
      }

      AssetImportPayloadDto response =
          objectMapper.readValue(cachedJson, AssetImportPayloadDto.class);
      if (response.getImportId() == null) {
        response.setImportId(importId);
      }
      return response;
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

  private void writeCachedResponse(String cacheKey, AssetImportPayloadDto response) {
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

  public void savePersistedImportResult(AssetPersistRedisDto persistedImport) {
    String redisKey =
        ASSET_IMPORT_RESULT_KEY_PREFIX
            + persistedImport.getUserId()
            + ":"
            + persistedImport.getImportId();
    try {
      redisTemplate
          .opsForValue()
          .set(redisKey, objectMapper.writeValueAsString(persistedImport), IMPORT_RESULT_TTL);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to persist asset import result", ex);
    } catch (RuntimeException ex) {
      log.error(
          REQUEST_CONFLICT + CACHE_KEY_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          "asset import result persistence failed",
          redisKey,
          ex);
      throw ex;
    }
  }

  private AssetImportPayloadDto extractCommonPayload(
      MultipartFile file, byte[] fileBytes, String fileName, String importId) {
    long startTime = System.nanoTime();
    log.info(
        REQUEST_START + IMPORT_QUEUE_STATUS,
        ASSET,
        IMPORT_ASSETS_ACTION,
        IMPORT_PROCESSING_START_MESSAGE,
        fileName,
        importExecutor.getActiveCount(),
        importExecutor.getQueue().size());
    try (InputStream inputStream = inputStream(file, fileBytes)) {
      assetAiValidationHelper.validateExtractionRequest(fileName, inputStream);
      String extractedText = FileDataExtractor.extractText(fileName, inputStream);
      assetAiValidationHelper.validateExtractedText(extractedText);
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      log.info(
          REQUEST_SUCCESS + IMPORT_PROCESSING_SUCCESS_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_PROCESSING_SUCCESS_MESSAGE,
          fileName,
          elapsedMillis,
          extractedText.length());
      return new AssetImportPayloadDto(importId, fileName, extractedText);
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

  private Future<AssetImportPayloadDto> submitImportTask(
      MultipartFile file, byte[] fileBytes, String fileName, String importId) {
    FutureTask<AssetImportPayloadDto> task =
        new FutureTask<>(() -> extractCommonPayload(file, fileBytes, fileName, importId));

    if (importExecutor.isShutdown()) {
      throw new IllegalStateException("Asset import queue is unavailable");
    }

    if (importExecutor.getQueue().offer(task)) {
      ensureWorkerScheduled();
      return task;
    }

    return submitDirectOrReject(task);
  }

  private InputStream inputStream(MultipartFile file, byte[] fileBytes) throws IOException {
    InputStream inputStream = file.getInputStream();
    return inputStream == null ? new ByteArrayInputStream(fileBytes) : inputStream;
  }

  private Future<AssetImportPayloadDto> submitDirectOrReject(
      FutureTask<AssetImportPayloadDto> task) {
    try {
      importExecutor.execute(task);
      return task;
    } catch (RejectedExecutionException ex) {
      awaitQueueCapacity(task);
      return task;
    }
  }

  private void awaitQueueCapacity(FutureTask<AssetImportPayloadDto> task) {
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
