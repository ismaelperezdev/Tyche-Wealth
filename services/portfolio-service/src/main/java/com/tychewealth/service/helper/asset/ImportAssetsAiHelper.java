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
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.utils.AiUtils;
import com.tychewealth.utils.Utils;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private final Duration aiRequestTimeout;
  private final Duration aiQueueOfferTimeout;
  private static final Pattern HOLDING_START_PATTERN =
      Pattern.compile(
          "^(?<quantity>\\d[\\d.,]*)\\s+(?<unit>[\\p{L}]{1,12}\\.?)(?:\\s+(?<name>.+))?$");
  private static final Pattern ISIN_PATTERN = Pattern.compile("\\b[A-Z]{2}[A-Z0-9]{10}\\b");
  private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{2}[./-]\\d{2}[./-]\\d{4}$");
  private static final Pattern NUMERIC_VALUE_PATTERN =
      Pattern.compile("^(?:[A-Z]{3}\\s*)?\\d[\\d.,]*(?:\\s*[A-Z]{3})?$");

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
      @Value("${app.asset.import.ai.queue.capacity:20}") int queueCapacity,
      @Value("${app.asset.import.ai.queue.offer-timeout-seconds:5}")
          long queueOfferTimeoutSeconds) {
    this.aiClient = aiClient;
    this.assetValidationHelper = assetValidationHelper;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.aiRequestTimeout =
        Duration.ofSeconds(Math.max(1L, assetValidationHelper.aiTimeoutSeconds()));
    this.aiQueueOfferTimeout =
        Duration.ofSeconds(Math.max(1L, queueOfferTimeoutSeconds)).compareTo(aiRequestTimeout) > 0
            ? aiRequestTimeout
            : Duration.ofSeconds(Math.max(1L, queueOfferTimeoutSeconds));
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
    String cachedResponse = null;
    try {
      cachedResponse = redisTemplate.opsForValue().get(cacheKey);
    } catch (RuntimeException ex) {
      log.error(
          REQUEST_CONFLICT + " cacheKey={} " + MODEL_TYPE_CONTEXT,
          ASSET,
          IMPORT_ASSETS_ACTION,
          "asset import ai cache read failed",
          cacheKey,
          modelType,
          ex);
    }
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

    long deadlineNanos = System.nanoTime() + aiRequestTimeout.toNanos();
    Future<String> future;
    try {
      future = aiExecutor.submit(() -> callAi(prompt, modelType));
    } catch (RejectedExecutionException ex) {
      throw Utils.rateLimited("AI queue is full");
    }
    try {
      long remainingTimeoutNanos = remainingTimeout(deadlineNanos);
      String response = future.get(remainingTimeoutNanos, TimeUnit.NANOSECONDS);
      log.info(
          REQUEST_SUCCESS + AI_QUEUE_STATUS,
          ASSET,
          IMPORT_ASSETS_ACTION,
          AI_REQUEST_COMPLETED_MESSAGE,
          modelType,
          aiExecutor.getActiveCount(),
          aiExecutor.getQueue().size());
      try {
        redisTemplate.opsForValue().set(cacheKey, response, AI_CACHE_TTL);
      } catch (RuntimeException ex) {
        log.error(
            REQUEST_CONFLICT + " cacheKey={} " + MODEL_TYPE_CONTEXT,
            ASSET,
            IMPORT_ASSETS_ACTION,
            "asset import ai cache write failed",
            cacheKey,
            modelType,
            ex);
      }
      return response;
    } catch (InterruptedException ex) {
      future.cancel(true);
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
    } catch (RuntimeException ex) {
      future.cancel(true);
      throw ex;
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

  private long remainingTimeout(long deadlineNanos) {
    long remainingNanos = deadlineNanos - System.nanoTime();
    if (remainingNanos <= 0) {
      throw assetValidationHelper.aiTimeoutExceeded(aiRequestTimeout.toSeconds());
    }
    return remainingNanos;
  }

  private String buildCacheKey(String prompt, AiModelTypeEnum modelType) {
    return AI_CACHE_KEY_PREFIX
        + modelType.name()
        + ":"
        + com.tychewealth.utils.Utils.sha256Hex(prompt);
  }

  public List<AssetImportCandidateDto> parseAiAssets(String aiResponse) {
    return parseAiAssets(null, aiResponse);
  }

  public List<AssetImportCandidateDto> parseAiAssets(String extractedText, String aiResponse) {
    try {
      JavaType type =
          objectMapper
              .getTypeFactory()
              .constructCollectionType(List.class, AssetImportCandidateDto.class);
      String sanitizedResponse = AiUtils.sanitizeAiResponse(aiResponse);
      List<AssetImportCandidateDto> parsedAssets = objectMapper.readValue(sanitizedResponse, type);
      if (parsedAssets == null) {
        parsedAssets = Collections.emptyList();
      }
      List<AssetImportCandidateDto> assets =
          parsedAssets.stream().filter(Objects::nonNull).filter(this::hasMeaningfulField).toList();
      assets = mergeWithDeterministicExtraction(extractedText, assets);
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

  private boolean hasMeaningfulField(AssetImportCandidateDto asset) {
    return trimToNull(asset.getName()) != null
        || trimToNull(asset.getSymbol()) != null
        || asset.getAssetType() != null
        || asset.getQuantity() != null
        || asset.getAveragePrice() != null
        || asset.getCurrency() != null;
  }

  private List<AssetImportCandidateDto> mergeWithDeterministicExtraction(
      String extractedText, List<AssetImportCandidateDto> aiAssets) {
    if (extractedText == null || extractedText.isBlank() || aiAssets.isEmpty()) {
      return aiAssets;
    }

    List<AssetImportCandidateDto> extractedAssets = extractAssetsFromStatement(extractedText);
    if (extractedAssets.size() != aiAssets.size()) {
      return aiAssets;
    }

    List<AssetImportCandidateDto> mergedAssets = new ArrayList<>(aiAssets.size());
    for (int index = 0; index < aiAssets.size(); index++) {
      mergedAssets.add(mergeAsset(aiAssets.get(index), extractedAssets.get(index)));
    }
    return mergedAssets;
  }

  private List<AssetImportCandidateDto> extractAssetsFromStatement(String extractedText) {
    List<String> lines =
        extractedText.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
    List<List<String>> blocks = new ArrayList<>();
    List<String> currentBlock = null;

    for (String line : lines) {
      if (isHoldingSectionTerminator(line)) {
        addCurrentBlock(blocks, currentBlock);
        return buildExtractedAssets(blocks, lines);
      }

      currentBlock = appendLineToCurrentBlock(blocks, currentBlock, line);
    }

    addCurrentBlock(blocks, currentBlock);
    return buildExtractedAssets(blocks, lines);
  }

  private List<AssetImportCandidateDto> buildExtractedAssets(
      List<List<String>> blocks, List<String> lines) {
    CurrencyCodeEnum statementCurrency = detectStatementCurrency(lines);
    return blocks.stream()
        .map(block -> parseHoldingBlock(block, statementCurrency))
        .filter(Objects::nonNull)
        .filter(this::hasMeaningfulField)
        .toList();
  }

  private List<String> appendLineToCurrentBlock(
      List<List<String>> blocks, List<String> currentBlock, String line) {
    List<String> updatedBlock = currentBlock;
    if (isHoldingStart(line)) {
      addCurrentBlock(blocks, currentBlock);
      updatedBlock = new ArrayList<>();
    }

    if (updatedBlock != null) {
      updatedBlock.add(line);
    }
    return updatedBlock;
  }

  private void addCurrentBlock(List<List<String>> blocks, List<String> currentBlock) {
    if (currentBlock != null && !currentBlock.isEmpty()) {
      blocks.add(currentBlock);
    }
  }

  private AssetImportCandidateDto parseHoldingBlock(
      List<String> blockLines, CurrencyCodeEnum statementCurrency) {
    if (blockLines.isEmpty()) {
      return null;
    }

    Matcher startMatcher = HOLDING_START_PATTERN.matcher(blockLines.get(0));
    if (!startMatcher.matches()) {
      return null;
    }

    BigDecimal quantity = parseLocalizedNumber(startMatcher.group("quantity"));
    List<String> nameParts = new ArrayList<>();
    String initialName = trimToNull(startMatcher.group("name"));
    if (initialName != null) {
      nameParts.add(initialName);
    }

    String isin = null;
    List<BigDecimal> valueCandidates = new ArrayList<>();

    for (int index = 1; index < blockLines.size(); index++) {
      String line = blockLines.get(index).trim();
      isin = processHoldingLine(line, isin, valueCandidates, nameParts);
    }

    String name = trimToNull(String.join(" ", nameParts).replaceAll("\\s+", " "));
    BigDecimal averagePrice = valueCandidates.isEmpty() ? null : valueCandidates.get(0);
    AssetTypeEnum assetType = inferAssetType(name);

    return new AssetImportCandidateDto(
        name, isin, assetType, quantity, averagePrice, statementCurrency);
  }

  private String processHoldingLine(
      String line, String currentIsin, List<BigDecimal> valueCandidates, List<String> nameParts) {
    if (line.isBlank() || isMetadataLine(line) || DATE_PATTERN.matcher(line).matches()) {
      return currentIsin;
    }

    Matcher isinMatcher = ISIN_PATTERN.matcher(line);
    if (isinMatcher.find()) {
      return isinMatcher.group();
    }

    if (NUMERIC_VALUE_PATTERN.matcher(line).matches()) {
      BigDecimal number = parseLocalizedNumber(line.replaceAll("[A-Z]{3}", "").trim());
      if (number != null) {
        valueCandidates.add(number);
      }
      return currentIsin;
    }

    nameParts.add(line);
    return currentIsin;
  }

  private AssetImportCandidateDto mergeAsset(
      AssetImportCandidateDto aiAsset, AssetImportCandidateDto extractedAsset) {
    String symbol = choosePreferredSymbol(aiAsset.getSymbol(), extractedAsset.getSymbol());
    return new AssetImportCandidateDto(
        firstNonBlank(extractedAsset.getName(), aiAsset.getName()),
        symbol,
        chooseAssetType(extractedAsset.getAssetType(), aiAsset.getAssetType()),
        extractedAsset.getQuantity() != null ? extractedAsset.getQuantity() : aiAsset.getQuantity(),
        extractedAsset.getAveragePrice() != null
            ? extractedAsset.getAveragePrice()
            : aiAsset.getAveragePrice(),
        extractedAsset.getCurrency() != null
            ? extractedAsset.getCurrency()
            : aiAsset.getCurrency());
  }

  private String choosePreferredSymbol(String aiSymbol, String extractedSymbol) {
    if (looksLikeTicker(aiSymbol) && looksLikeIsin(extractedSymbol)) {
      return aiSymbol;
    }
    return firstNonBlank(extractedSymbol, aiSymbol);
  }

  private AssetTypeEnum chooseAssetType(AssetTypeEnum extractedType, AssetTypeEnum aiType) {
    if (extractedType != null && extractedType != AssetTypeEnum.OTHER) {
      return extractedType;
    }
    return aiType != null ? aiType : extractedType;
  }

  private CurrencyCodeEnum detectStatementCurrency(List<String> lines) {
    for (String line : lines) {
      String upper = line.toUpperCase();
      if (!(upper.contains("EUR")
          || upper.contains("USD")
          || upper.contains("GBP")
          || upper.contains("CHF")
          || upper.contains("JPY")
          || upper.contains("CNY"))) {
        continue;
      }
      if (upper.contains("COTIZ")
          || upper.contains("PRICE")
          || upper.contains("QUOTE")
          || upper.contains("VALOR")
          || upper.contains("AMOUNT")
          || upper.contains("IMPORTE")) {
        for (CurrencyCodeEnum currency : CurrencyCodeEnum.values()) {
          if (currency != CurrencyCodeEnum.UNKNOWN && upper.contains(currency.name())) {
            return currency;
          }
        }
      }
    }
    return null;
  }

  private boolean isHoldingStart(String line) {
    Matcher matcher = HOLDING_START_PATTERN.matcher(line);
    return matcher.matches() && parseLocalizedNumber(matcher.group("quantity")) != null;
  }

  private boolean isHoldingSectionTerminator(String line) {
    String normalized = line.toLowerCase();
    return normalized.startsWith("número de posiciones")
        || normalized.startsWith("numero de posiciones")
        || normalized.startsWith("ten en cuenta")
        || normalized.startsWith("los precios mostrados")
        || normalized.startsWith("total");
  }

  private boolean isMetadataLine(String line) {
    String normalized = line.toLowerCase();
    return normalized.startsWith("isin:")
        || normalized.startsWith("cuenta de valores")
        || normalized.startsWith("país de custodia")
        || normalized.startsWith("pais de custodia")
        || normalized.startsWith("country of custody")
        || normalized.startsWith("custody country")
        || normalized.startsWith("page ")
        || normalized.startsWith("página ")
        || normalized.startsWith("pagina ")
        || normalized.startsWith("fecha ")
        || normalized.startsWith("date ");
  }

  private AssetTypeEnum inferAssetType(String name) {
    String normalized = name == null ? "" : name.toLowerCase();
    if (normalized.isBlank()) {
      return null;
    }
    if (normalized.contains("bitcoin")
        || normalized.contains("ethereum")
        || normalized.contains("crypto")
        || normalized.contains("token")) {
      return AssetTypeEnum.CRYPTO;
    }
    if (normalized.contains("bond")
        || normalized.contains("note")
        || normalized.contains("treasury")
        || normalized.contains("debenture")
        || normalized.contains("coupon")) {
      return AssetTypeEnum.BOND;
    }
    if (normalized.contains("etf")
        || normalized.contains("etc")
        || normalized.contains("etn")
        || normalized.contains("ucits")
        || normalized.contains("fund")
        || normalized.contains("ishares")
        || normalized.contains("wisdomtree")
        || normalized.contains("physical metals")) {
      return AssetTypeEnum.ETF;
    }
    if (normalized.contains("corp")
        || normalized.contains("inc")
        || normalized.contains("plc")
        || normalized.contains("class a")
        || normalized.contains("registered shares")
        || normalized.contains("reg.shares")) {
      return AssetTypeEnum.STOCK;
    }
    return AssetTypeEnum.OTHER;
  }

  private BigDecimal parseLocalizedNumber(String rawNumber) {
    String normalized = trimToNull(rawNumber);
    if (normalized == null) {
      return null;
    }
    normalized = normalized.replace(" ", "");
    if (normalized.contains(",") && normalized.contains(".")) {
      if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) {
        normalized = normalized.replace(".", "").replace(',', '.');
      } else {
        normalized = normalized.replace(",", "");
      }
    } else if (normalized.contains(",")) {
      normalized = normalized.replace(',', '.');
    }

    try {
      return new BigDecimal(normalized);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private boolean looksLikeTicker(String symbol) {
    String normalized = trimToNull(symbol);
    return normalized != null
        && normalized.matches("(?=.*[A-Z])[A-Z0-9.\\-]{1,6}")
        && !looksLikeIsin(normalized);
  }

  private boolean looksLikeIsin(String symbol) {
    String normalized = trimToNull(symbol);
    return normalized != null && ISIN_PATTERN.matcher(normalized).matches();
  }

  private String firstNonBlank(String primary, String fallback) {
    String preferred = trimToNull(primary);
    return preferred != null ? preferred : trimToNull(fallback);
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  @PreDestroy
  void shutdown() {
    aiExecutor.shutdown();
  }

  private final class BlockingQueuePolicy implements RejectedExecutionHandler {

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
        boolean enqueued =
            executor
                .getQueue()
                .offer(runnable, aiQueueOfferTimeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!enqueued) {
          throw new RejectedExecutionException(
              "Timed out while waiting for ai queue after "
                  + aiQueueOfferTimeout.toSeconds()
                  + " seconds");
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RejectedExecutionException("Interrupted while waiting for ai queue", ex);
      }
    }
  }
}
