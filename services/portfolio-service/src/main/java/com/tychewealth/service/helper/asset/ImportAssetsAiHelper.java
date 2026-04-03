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

import com.tychewealth.client.AiClient;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImportAssetsAiHelper {

  private final AiClient aiClient;
  private final ThreadPoolExecutor aiExecutor;

  public ImportAssetsAiHelper(
      AiClient aiClient,
      @Value("${app.asset.import.ai.queue.max-concurrency:1}") int maxConcurrency,
      @Value("${app.asset.import.ai.queue.capacity:20}") int queueCapacity) {
    this.aiClient = aiClient;
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

  private String execute(String prompt, AiModelTypeEnum modelType) {
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
      String response = future.get();
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
      Thread.currentThread().interrupt();
      log.warn(
          REQUEST_CONFLICT + MODEL_TYPE_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          AI_REQUEST_INTERRUPTED_MESSAGE,
          modelType,
          ex);
      throw new IllegalStateException("AI processing was interrupted", ex);
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
