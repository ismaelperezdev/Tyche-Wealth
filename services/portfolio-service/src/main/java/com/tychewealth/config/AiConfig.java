package com.tychewealth.config;

import com.tychewealth.ai.AiQueueBlockingPolicy;
import com.tychewealth.dto.ai.AiPropertiesDto;
import com.tychewealth.service.helper.asset.AssetValidationHelper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiPropertiesDto.class)
public class AiConfig {

  @Bean
  public HttpClient aiHttpClient(AiPropertiesDto aiProperties) {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(aiProperties.connectTimeoutSeconds()))
        .build();
  }

  @Bean
  public Duration assetImportAiRequestTimeout(AssetValidationHelper assetValidationHelper) {
    return Duration.ofSeconds(Math.max(1L, assetValidationHelper.aiTimeoutSeconds()));
  }

  @Bean(destroyMethod = "shutdown")
  public ThreadPoolExecutor assetImportAiExecutor(
      Duration assetImportAiRequestTimeout,
      @Value("${app.asset.import.ai.queue.max-concurrency:1}") int maxConcurrency,
      @Value("${app.asset.import.ai.queue.capacity:20}") int queueCapacity,
      @Value("${app.asset.import.ai.queue.offer-timeout-seconds:5}")
          long queueOfferTimeoutSeconds) {
    Duration requestedQueueOfferTimeout =
        Duration.ofSeconds(Math.max(1L, queueOfferTimeoutSeconds));
    Duration aiQueueOfferTimeout =
        requestedQueueOfferTimeout.compareTo(assetImportAiRequestTimeout) > 0
            ? assetImportAiRequestTimeout
            : requestedQueueOfferTimeout;
    int executorConcurrency = Math.max(1, maxConcurrency);
    int executorQueueCapacity = Math.max(1, queueCapacity);

    return new ThreadPoolExecutor(
        executorConcurrency,
        executorConcurrency,
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(executorQueueCapacity),
        new AiQueueBlockingPolicy(aiQueueOfferTimeout));
  }
}
