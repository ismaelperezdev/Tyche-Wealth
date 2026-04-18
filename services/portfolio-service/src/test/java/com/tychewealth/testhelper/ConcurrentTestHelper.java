package com.tychewealth.testhelper;

import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.testhelper.AuthTestHelper.createAuthorizationHeader;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequest;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public final class ConcurrentTestHelper {

  private ConcurrentTestHelper() {}

  @SafeVarargs
  public static <T> List<T> runConcurrently(Callable<T>... tasks) throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(tasks.length);
    CountDownLatch ready = new CountDownLatch(tasks.length);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<T>> futures = new ArrayList<>();
      for (Callable<T> task : tasks) {
        futures.add(
            executorService.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new TimeoutException("Timed out waiting to start concurrent tasks");
                  }
                  return task.call();
                }));
      }

      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();

      List<T> results = new ArrayList<>();
      for (Future<T> future : futures) {
        results.add(getFutureValue(future));
      }
      return results;
    } finally {
      executorService.shutdownNow();
    }
  }

  private static <T> T getFutureValue(Future<T> future) throws Exception {
    try {
      return future.get(10, TimeUnit.SECONDS);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      throw ex;
    }
  }

  public static IntegrationResponse executeCreate(MockMvc mockMvc, long userId, String requestBody)
      throws Exception {
    MvcResult result = createRequest(mockMvc, String.valueOf(userId), requestBody).andReturn();
    return new IntegrationResponse(
        result.getResponse().getStatus(), result.getResponse().getContentAsString());
  }

  public static IntegrationResponse executeImport(
      MockMvc mockMvc,
      long userId,
      String endpoint,
      String fileName,
      String contentType,
      byte[] bytes)
      throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", fileName, contentType, bytes);

    MvcResult result =
        mockMvc
            .perform(
                multipart(endpoint)
                    .file(file)
                    .header(AUTHORIZATION_HEADER, createAuthorizationHeader(userId)))
            .andReturn();

    return new IntegrationResponse(
        result.getResponse().getStatus(), result.getResponse().getContentAsString());
  }

  public record IntegrationResponse(int status, String body) {}
}
