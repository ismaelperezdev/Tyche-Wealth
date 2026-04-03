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

import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.utils.FileDataExtractor;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class ImportAssetsHelper {

  private final AssetValidationHelper assetValidationHelper;
  private final ThreadPoolExecutor importExecutor;

  public ImportAssetsHelper(
      AssetValidationHelper assetValidationHelper,
      @Value("${app.asset.import.queue.max-concurrency:4}") int maxConcurrency,
      @Value("${app.asset.import.queue.capacity:50}") int queueCapacity) {
    this.assetValidationHelper = assetValidationHelper;
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
      AssetImportResponseDto response = future.get();
      log.info(
          REQUEST_SUCCESS + IMPORT_QUEUE_STATUS,
          ASSET,
          IMPORT_ASSETS_ACTION,
          IMPORT_COMPLETED_MESSAGE,
          fileName,
          importExecutor.getActiveCount(),
          importExecutor.getQueue().size());
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
      return new AssetImportResponseDto(fileName, extractedText, null);
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
