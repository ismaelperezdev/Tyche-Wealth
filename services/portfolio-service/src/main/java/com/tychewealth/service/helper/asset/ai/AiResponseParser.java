package com.tychewealth.service.helper.asset.ai;

import static com.tychewealth.constants.AiConstants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.ai.AiResponseSanitizer;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.exception.AssetImportException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.helper.asset.ai.support.AiResponseParserSupport;
import com.tychewealth.utils.Utils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class AiResponseParser {

  private final AssetAiValidationHelper assetAiValidationHelper;
  private final ObjectMapper objectMapper;
  private final List<String> holdingSectionTerminators;
  private final List<String> metadataPrefixes;
  private final Map<AssetTypeEnum, List<String>> assetTypeKeywords;
  private final Set<String> statementCurrencyCodes;
  private final Set<String> statementCurrencyContextKeywords;
  private final AiResponseParserSupport aiResponseParserSupport;

  public AiResponseParser(
      ObjectMapper objectMapper,
      AssetAiValidationHelper assetAiValidationHelper,
      AiResponseParserSupport aiResponseParserSupport) {
    this.assetAiValidationHelper = assetAiValidationHelper;
    this.objectMapper = objectMapper;
    this.aiResponseParserSupport = aiResponseParserSupport;
    this.holdingSectionTerminators = aiResponseParserSupport.getHoldingSectionTerminators();
    this.metadataPrefixes = aiResponseParserSupport.getMetadataPrefixes();
    this.assetTypeKeywords = aiResponseParserSupport.getAssetTypeKeywords();
    this.statementCurrencyCodes = aiResponseParserSupport.getStatementCurrencyCodes();
    this.statementCurrencyContextKeywords =
        aiResponseParserSupport.getStatementCurrencyContextKeywords();
  }

  public List<AssetImportCandidateDto> parseAiAssets(String extractedText, String aiResponse) {
    try {
      JavaType type =
          objectMapper
              .getTypeFactory()
              .constructCollectionType(List.class, AssetImportCandidateDto.class);
      String sanitizedResponse = AiResponseSanitizer.sanitizeAiResponse(aiResponse);
      List<AssetImportCandidateDto> parsedAssets = objectMapper.readValue(sanitizedResponse, type);

      if (parsedAssets == null) {
        parsedAssets = Collections.emptyList();
      }

      List<AssetImportCandidateDto> assets =
          parsedAssets.stream()
              .filter(Objects::nonNull)
              .filter(aiResponseParserSupport::hasMeaningfulField)
              .toList();
      assets = mergeWithDeterministicExtraction(extractedText, assets);
      assetAiValidationHelper.validateDetectedAssetsCount(assets.size());

      return assets;
    } catch (JsonProcessingException ex) {
      throw new AssetImportException(
          ErrorDefinition.ASSET_IMPORT_AI_RESPONSE_INVALID,
          Map.of("error", ex.getOriginalMessage()),
          HttpStatus.BAD_REQUEST);
    }
  }

  private List<AssetImportCandidateDto> mergeWithDeterministicExtraction(
      String extractedText, List<AssetImportCandidateDto> aiAssets) {
    List<AssetImportCandidateDto> extractedAssets;

    if (extractedText == null || extractedText.isBlank() || aiAssets.isEmpty()) {
      return aiAssets;
    }

    extractedAssets = extractAssetsFromStatement(extractedText);
    if (extractedAssets.size() != aiAssets.size()) {
      return aiAssets;
    }

    return IntStream.range(0, aiAssets.size())
        .mapToObj(
            index ->
                aiResponseParserSupport.mergeAssetPair(
                    aiAssets.get(index), extractedAssets.get(index)))
        .toList();
  }

  private List<AssetImportCandidateDto> extractAssetsFromStatement(String extractedText) {
    List<String> lines =
        extractedText.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
    List<List<String>> blocks = new ArrayList<>();
    List<String> currentBlock = null;

    for (String line : lines) {
      String normalizedLine = line.toLowerCase(Locale.ROOT);
      if (holdingSectionTerminators.stream().anyMatch(normalizedLine::startsWith)) {
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

    CurrencyCodeEnum statementCurrency =
        lines.stream()
            .map(line -> line.toUpperCase(Locale.ROOT))
            .filter(upper -> statementCurrencyCodes.stream().anyMatch(upper::contains))
            .filter(upper -> statementCurrencyContextKeywords.stream().anyMatch(upper::contains))
            .flatMap(
                upper ->
                    java.util.Arrays.stream(CurrencyCodeEnum.values())
                        .filter(currency -> currency != CurrencyCodeEnum.UNKNOWN)
                        .filter(currency -> upper.contains(currency.name())))
            .findFirst()
            .orElse(null);

    return blocks.stream()
        .map(block -> parseHoldingBlock(block, statementCurrency))
        .filter(Objects::nonNull)
        .filter(aiResponseParserSupport::hasMeaningfulField)
        .toList();
  }

  private List<String> appendLineToCurrentBlock(
      List<List<String>> blocks, List<String> currentBlock, String line) {
    List<String> updatedBlock = currentBlock;
    Matcher matcher = HOLDING_START_PATTERN.matcher(line);

    if (matcher.matches() && Utils.parseLocalizedNumber(matcher.group("quantity")) != null) {
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
    Matcher startMatcher;
    BigDecimal quantity;
    List<String> nameParts = new ArrayList<>();
    String initialName;
    String isin;
    List<BigDecimal> valueCandidates = new ArrayList<>();
    String name;
    BigDecimal averagePrice;
    String normalizedName;
    AssetTypeEnum assetType = null;

    if (blockLines.isEmpty()) {
      return null;
    }

    startMatcher = HOLDING_START_PATTERN.matcher(blockLines.getFirst());
    if (!startMatcher.matches()) {
      return null;
    }

    quantity = Utils.parseLocalizedNumber(startMatcher.group("quantity"));
    initialName = Utils.trimToNull(startMatcher.group("name"));

    if (initialName != null) {
      nameParts.add(initialName);
    }

    isin =
        blockLines.stream()
            .skip(1)
            .map(String::trim)
            .reduce(
                null,
                (currentIsin, line) ->
                    processHoldingLine(line, currentIsin, valueCandidates, nameParts),
                (leftIsin, rightIsin) -> rightIsin != null ? rightIsin : leftIsin);

    name = Utils.trimToNull(String.join(" ", nameParts).replaceAll("\\s+", " "));
    averagePrice = valueCandidates.isEmpty() ? null : valueCandidates.getFirst();
    normalizedName = name == null ? "" : name.toLowerCase(Locale.ROOT);

    assetType =
        normalizedName.isBlank()
            ? null
            : INFERRED_ASSET_TYPE_ORDER.stream()
                .filter(
                    candidateType ->
                        assetTypeKeywords.getOrDefault(candidateType, List.of()).stream()
                            .anyMatch(normalizedName::contains))
                .findFirst()
                .orElse(AssetTypeEnum.OTHER);

    return new AssetImportCandidateDto(
        name, isin, assetType, quantity, averagePrice, statementCurrency);
  }

  private String processHoldingLine(
      String line, String currentIsin, List<BigDecimal> valueCandidates, List<String> nameParts) {
    String normalizedLine = line.toLowerCase(Locale.ROOT);
    Matcher isinMatcher;
    BigDecimal number;

    if (line.isBlank()
        || metadataPrefixes.stream().anyMatch(normalizedLine::startsWith)
        || DATE_PATTERN.matcher(line).matches()) {
      return currentIsin;
    }

    isinMatcher = ISIN_PATTERN.matcher(line);
    if (isinMatcher.find()) {
      return isinMatcher.group();
    }

    if (!NUMERIC_VALUE_PATTERN.matcher(line).matches()) {
      nameParts.add(line);
      return currentIsin;
    }

    number = Utils.parseLocalizedNumber(line.replaceAll("[A-Z]{3}", "").trim());
    if (number != null) {
      valueCandidates.add(number);
    }
    return currentIsin;
  }
}
