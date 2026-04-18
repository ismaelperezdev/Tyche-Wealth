package com.tychewealth.service.helper.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.asset.AssetImportPayloadDto;
import com.tychewealth.testhelper.TestRedisSupport;
import com.tychewealth.testhelper.TestRedisSupport.InMemoryRedisState;
import com.tychewealth.utils.Utils;
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
    ImportAssetsTestContext context = createImportAssetsTestContext(1, 1);

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try {
      Future<AssetImportPayloadDto> first =
          executorService.submit(() -> context.helper().buildImportPayload(context.file()));

      assertTrue(
          context.firstStreamOpened().await(1, TimeUnit.SECONDS),
          "Timed out waiting for the first stream to open");

      Future<AssetImportPayloadDto> second =
          executorService.submit(() -> context.helper().buildImportPayload(context.file()));

      context.releaseFirstStream().countDown();

      AssetImportPayloadDto firstResponse = first.get();
      AssetImportPayloadDto secondResponse = second.get();

      assertNotNull(firstResponse);
      assertNotNull(secondResponse);
      assertEquals("positions.csv", firstResponse.getFileName());
      assertEquals("positions.csv", secondResponse.getFileName());
      assertEquals("ticker,quantity\nAAPL,10", firstResponse.getExtractedText());
      assertEquals("ticker,quantity\nAAPL,10", secondResponse.getExtractedText());
      assertEquals(1, context.inputStreamCalls().get());
    } finally {
      executorService.shutdownNow();
      context.helper().shutdown();
    }
  }

  @Test
  void buildImportPayloadReleasesInflightLockWhenTaskSubmissionFails() throws IOException {
    ImportAssetsTestContext context = createImportAssetsTestContext(0, 0);
    String cacheKey =
        "asset-import:payload:"
            + Utils.sha256Hex("positions.csv".toLowerCase() + ":" + Utils.sha256Hex(FILE_BYTES));
    String inflightKey =
        "asset-import:payload:inflight:" + cacheKey.substring("asset-import:payload:".length());

    context.helper().shutdown();

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> context.helper().buildImportPayload(context.file()));

    assertEquals("Asset import queue is unavailable", exception.getMessage());
    assertFalse(context.redisState().hasKey(inflightKey));
  }

  private ImportAssetsTestContext createImportAssetsTestContext(
      int firstStreamOpenedCount, int releaseFirstStreamCount) throws IOException {
    AssetValidationHelper validationHelper = new AssetValidationHelper(1024L, 10, 5000, 5, 5, 10);
    InMemoryRedisState redisState = new InMemoryRedisState();
    RedisTemplate<String, String> redisTemplate = TestRedisSupport.redisTemplate(redisState);
    AtomicInteger inputStreamCalls = new AtomicInteger();
    CountDownLatch firstStreamOpened = new CountDownLatch(firstStreamOpenedCount);
    CountDownLatch releaseFirstStream = new CountDownLatch(releaseFirstStreamCount);
    MultipartFile file = buildFile(inputStreamCalls, firstStreamOpened, releaseFirstStream);
    ImportAssetsHelper helper =
        new ImportAssetsHelper(
            validationHelper, redisTemplate, new ObjectMapper(), 1, 10, 10, 2, 25);
    return new ImportAssetsTestContext(
        validationHelper,
        redisState,
        redisTemplate,
        inputStreamCalls,
        firstStreamOpened,
        releaseFirstStream,
        file,
        helper);
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

  private record ImportAssetsTestContext(
      AssetValidationHelper validationHelper,
      InMemoryRedisState redisState,
      RedisTemplate<String, String> redisTemplate,
      AtomicInteger inputStreamCalls,
      CountDownLatch firstStreamOpened,
      CountDownLatch releaseFirstStream,
      MultipartFile file,
      ImportAssetsHelper helper) {}
}
