package com.tychewealth.service.helper.asset.ai.support;

import static com.tychewealth.constants.AiConstants.ISIN_PATTERN;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.utils.JsonResourceLoader;
import com.tychewealth.utils.Utils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AiResponseParserSupport {

  private final List<String> holdingSectionTerminators;
  private final List<String> metadataPrefixes;
  private final Map<AssetTypeEnum, List<String>> assetTypeKeywords;
  private final Set<String> statementCurrencyCodes;
  private final Set<String> statementCurrencyContextKeywords;

  public AiResponseParserSupport(ObjectMapper objectMapper) {
    this.holdingSectionTerminators =
        JsonResourceLoader.readStringList(
                objectMapper, "asset-import/holding-section-terminators.json")
            .stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .toList();
    this.metadataPrefixes =
        JsonResourceLoader.readStringList(objectMapper, "asset-import/metadata-prefixes.json")
            .stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .toList();
    this.assetTypeKeywords =
        JsonResourceLoader.readEnumKeyedStringListMap(
            objectMapper, "asset-import/asset-type-keywords.json", AssetTypeEnum.class);
    this.statementCurrencyCodes =
        JsonResourceLoader.readStringList(
                objectMapper, "asset-import/statement-currency-codes.json")
            .stream()
            .map(value -> value.toUpperCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    this.statementCurrencyContextKeywords =
        JsonResourceLoader.readStringList(
                objectMapper, "asset-import/statement-currency-context-keywords.json")
            .stream()
            .map(value -> value.toUpperCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
  }

  public boolean looksLikeIsin(String symbol) {
    String normalized = Utils.trimToNull(symbol);
    return normalized != null && ISIN_PATTERN.matcher(normalized).matches();
  }

  public String firstNonBlank(String primary, String fallback) {
    String preferred = Utils.trimToNull(primary);
    return preferred != null ? preferred : Utils.trimToNull(fallback);
  }

  public <T> T firstNonNull(T primary, T fallback) {
    return primary != null ? primary : fallback;
  }

  public boolean hasMeaningfulField(AssetImportCandidateDto asset) {
    return Utils.trimToNull(asset.getName()) != null
        || Utils.trimToNull(asset.getSymbol()) != null
        || asset.getAssetType() != null
        || asset.getQuantity() != null
        || asset.getAveragePrice() != null
        || asset.getCurrency() != null;
  }

  public AssetImportCandidateDto mergeAssetPair(
      AssetImportCandidateDto aiAsset, AssetImportCandidateDto extractedAsset) {
    String aiSymbol = aiAsset.getSymbol();
    String normalizedAiSymbol = Utils.trimToNull(aiSymbol);
    String extractedSymbol = extractedAsset.getSymbol();
    String symbol =
        normalizedAiSymbol != null
                && normalizedAiSymbol.matches("(?=.*[A-Z])[A-Z0-9.\\-]{1,6}")
                && !looksLikeIsin(normalizedAiSymbol)
                && looksLikeIsin(extractedSymbol)
            ? aiSymbol
            : firstNonBlank(extractedSymbol, aiSymbol);
    AssetTypeEnum extractedType = extractedAsset.getAssetType();
    AssetTypeEnum aiType = aiAsset.getAssetType();

    return buildMergedAsset(aiAsset, extractedAsset, symbol, extractedType, aiType);
  }

  public AssetImportCandidateDto buildMergedAsset(
      AssetImportCandidateDto aiAsset,
      AssetImportCandidateDto extractedAsset,
      String symbol,
      AssetTypeEnum extractedType,
      AssetTypeEnum aiType) {
    AssetTypeEnum resolvedAssetType =
        extractedType != null && extractedType != AssetTypeEnum.OTHER
            ? extractedType
            : firstNonNull(aiType, extractedType);

    return new AssetImportCandidateDto(
        firstNonBlank(extractedAsset.getName(), aiAsset.getName()),
        symbol,
        resolvedAssetType,
        firstNonNull(extractedAsset.getQuantity(), aiAsset.getQuantity()),
        firstNonNull(extractedAsset.getAveragePrice(), aiAsset.getAveragePrice()),
        firstNonNull(extractedAsset.getCurrency(), aiAsset.getCurrency()));
  }
}
