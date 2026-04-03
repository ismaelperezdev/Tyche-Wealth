package com.tychewealth.service.helper.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.asset.AssetImportResponseDto;
import com.tychewealth.testhelper.TestRedisSupport;
import com.tychewealth.testhelper.TestRedisSupport.InMemoryRedisState;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.multipart.MultipartFile;

class ImportAssetsHelperTest {

  private static final byte[] FILE_BYTES =
      "ticker,quantity\nAAPL,10".getBytes(StandardCharsets.UTF_8);

  @Test
  void buildImportPayloadDeduplicatesConcurrentRequestsForSameFile()
      throws InterruptedException, ExecutionException, IOException {
    AssetValidationHelper validationHelper = new AssetValidationHelper(1024L, 10, 5000, 5, 5, 10);
    InMemoryRedisState redisState = new InMemoryRedisState();
    RedisTemplate<String, String> redisTemplate = TestRedisSupport.redisTemplate(redisState);
    AtomicInteger inputStreamCalls = new AtomicInteger();
    CountDownLatch firstStreamOpened = new CountDownLatch(1);
    CountDownLatch releaseFirstStream = new CountDownLatch(1);
    MultipartFile file = buildFile(inputStreamCalls, firstStreamOpened, releaseFirstStream);
    ImportAssetsHelper helper =
        new ImportAssetsHelper(
            validationHelper, redisTemplate, new ObjectMapper(), 1, 10, 10, 2, 25);

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try {
      Future<AssetImportResponseDto> first =
          executorService.submit(() -> helper.buildImportPayload(file));

      firstStreamOpened.await(1, TimeUnit.SECONDS);

      Future<AssetImportResponseDto> second =
          executorService.submit(() -> helper.buildImportPayload(file));

      releaseFirstStream.countDown();

      AssetImportResponseDto firstResponse = first.get();
      AssetImportResponseDto secondResponse = second.get();

      assertNotNull(firstResponse);
      assertNotNull(secondResponse);
      assertEquals("positions.csv", firstResponse.getFileName());
      assertEquals("positions.csv", secondResponse.getFileName());
      assertEquals("ticker,quantity\nAAPL,10", firstResponse.getExtractedText());
      assertEquals("ticker,quantity\nAAPL,10", secondResponse.getExtractedText());
      assertEquals(1, inputStreamCalls.get());
    } finally {
      executorService.shutdownNow();
      helper.shutdown();
    }
  }

  private MultipartFile buildFile(
      AtomicInteger inputStreamCalls,
      CountDownLatch firstStreamOpened,
      CountDownLatch releaseFirstStream)
      throws IOException {
    MultipartFile file = Mockito.mock(MultipartFile.class);

    when(file.getOriginalFilename()).thenReturn("positions.csv");
    when(file.getBytes()).thenReturn(FILE_BYTES);
    when(file.getSize()).thenReturn((long) FILE_BYTES.length);
    when(file.isEmpty()).thenReturn(false);
    when(file.getInputStream())
        .thenAnswer(
            invocation -> {
              int currentCall = inputStreamCalls.incrementAndGet();
              if (currentCall == 1) {
                firstStreamOpened.countDown();
                if (!releaseFirstStream.await(1, TimeUnit.SECONDS)) {
                  throw new IllegalStateException("Timed out waiting to release first stream");
                }
              }
              return new ByteArrayInputStream(FILE_BYTES);
            });

    return file;
  }
}
