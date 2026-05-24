package com.tychewealth.testdata;

import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_CORE;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_20_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_200_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_GREATER_THAN_0;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_11_INTEGER_DIGITS_AND_8_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_15_INTEGER_DIGITS_AND_4_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_NULL;

import com.tychewealth.dto.asset.AssetImportCandidateDto;
import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class AssetTestData {

  public static final String TEST_ASSET_NAME_APPLE = "Apple Inc.";
  public static final String TEST_ASSET_SYMBOL_AAPL = "AAPL";
  public static final String TEST_ASSET_FILE_NAME = "positions.csv";
  public static final String TEST_ASSET_CONTENT_TYPE_CSV = "text/csv";
  public static final String TEST_ASSET_EXTRACTED_TEXT = "ticker,quantity\nAAPL,10";
  public static final BigDecimal TEST_ASSET_QUANTITY = new BigDecimal("10.00000000");
  public static final BigDecimal TEST_ASSET_AVERAGE_PRICE = new BigDecimal("150.0000");
  public static final BigDecimal TEST_ASSET_RESPONSE_QUANTITY = new BigDecimal("10");
  public static final BigDecimal TEST_ASSET_RESPONSE_AVERAGE_PRICE = new BigDecimal("150.00");

  private static final BigDecimal TEST_ZERO_QUANTITY = new BigDecimal("0.00000000");
  private static final BigDecimal TEST_INVALID_QUANTITY_PRECISION =
      new BigDecimal("123456789012.123456789");
  private static final BigDecimal TEST_ZERO_AVERAGE_PRICE = new BigDecimal("0.0000");
  private static final BigDecimal TEST_INVALID_AVERAGE_PRICE_PRECISION =
      new BigDecimal("1234567890123456.12345");

  public static final String AI_RESPONSE =
      """
      [
        {
          "name":"Apple Inc.",
          "symbol":"AAPL",
          "assetType":"STOCK",
          "quantity":10,
          "averagePrice":150.00,
          "currency":"USD"
        }
      ]
      """;

  private AssetTestData() {}

  public static Stream<Arguments> invalidBeanValidationCases() {
    return Stream.of(
        Arguments.of(invalidCreate(dto -> dto.setName(" ")), "name", MUST_NOT_BE_BLANK),
        Arguments.of(
            invalidCreate(dto -> dto.setName("AB")), "name", MUST_BE_BETWEEN_3_AND_200_CHARACTERS),
        Arguments.of(
            invalidCreate(dto -> dto.setSymbol("X".repeat(21))),
            "symbol",
            MUST_BE_AT_MOST_20_CHARACTERS),
        Arguments.of(invalidCreate(dto -> dto.setAssetType(null)), "assetType", MUST_NOT_BE_NULL),
        Arguments.of(invalidCreate(dto -> dto.setQuantity(null)), "quantity", MUST_NOT_BE_NULL),
        Arguments.of(
            invalidCreate(dto -> dto.setQuantity(TEST_ZERO_QUANTITY)),
            "quantity",
            MUST_BE_GREATER_THAN_0),
        Arguments.of(
            invalidCreate(dto -> dto.setQuantity(TEST_INVALID_QUANTITY_PRECISION)),
            "quantity",
            MUST_HAVE_UP_TO_11_INTEGER_DIGITS_AND_8_DECIMALS),
        Arguments.of(
            invalidCreate(dto -> dto.setAveragePrice(null)), "averagePrice", MUST_NOT_BE_NULL),
        Arguments.of(
            invalidCreate(dto -> dto.setAveragePrice(TEST_ZERO_AVERAGE_PRICE)),
            "averagePrice",
            MUST_BE_GREATER_THAN_0),
        Arguments.of(
            invalidCreate(dto -> dto.setAveragePrice(TEST_INVALID_AVERAGE_PRICE_PRECISION)),
            "averagePrice",
            MUST_HAVE_UP_TO_15_INTEGER_DIGITS_AND_4_DECIMALS),
        Arguments.of(invalidCreate(dto -> dto.setCurrency(null)), "currency", MUST_NOT_BE_NULL),
        Arguments.of(invalidUpdate(dto -> dto.setName(" ")), "name", MUST_NOT_BE_BLANK),
        Arguments.of(
            invalidUpdate(dto -> dto.setName("AB")), "name", MUST_BE_BETWEEN_3_AND_200_CHARACTERS),
        Arguments.of(invalidUpdate(dto -> dto.setSymbol(" ")), "symbol", MUST_NOT_BE_BLANK),
        Arguments.of(
            invalidUpdate(dto -> dto.setSymbol("X".repeat(21))),
            "symbol",
            MUST_BE_AT_MOST_20_CHARACTERS),
        Arguments.of(
            invalidUpdate(dto -> dto.setQuantity(TEST_ZERO_QUANTITY)),
            "quantity",
            MUST_BE_GREATER_THAN_0),
        Arguments.of(
            invalidUpdate(dto -> dto.setQuantity(TEST_INVALID_QUANTITY_PRECISION)),
            "quantity",
            MUST_HAVE_UP_TO_11_INTEGER_DIGITS_AND_8_DECIMALS),
        Arguments.of(
            invalidUpdate(dto -> dto.setAveragePrice(TEST_ZERO_AVERAGE_PRICE)),
            "averagePrice",
            MUST_BE_GREATER_THAN_0),
        Arguments.of(
            invalidUpdate(dto -> dto.setAveragePrice(TEST_INVALID_AVERAGE_PRICE_PRECISION)),
            "averagePrice",
            MUST_HAVE_UP_TO_15_INTEGER_DIGITS_AND_4_DECIMALS),
        Arguments.of(
            invalidEntity(entity -> entity.setPortfolio(null)), "portfolio", MUST_NOT_BE_NULL),
        Arguments.of(invalidEntity(entity -> entity.setName(" ")), "name", MUST_NOT_BE_BLANK),
        Arguments.of(
            invalidEntity(entity -> entity.setName("AB")),
            "name",
            MUST_BE_BETWEEN_3_AND_200_CHARACTERS),
        Arguments.of(invalidEntity(entity -> entity.setSymbol(" ")), "symbol", MUST_NOT_BE_BLANK),
        Arguments.of(
            invalidEntity(entity -> entity.setSymbol("X".repeat(21))),
            "symbol",
            MUST_BE_AT_MOST_20_CHARACTERS),
        Arguments.of(
            invalidEntity(entity -> entity.setAssetType(null)), "assetType", MUST_NOT_BE_NULL),
        Arguments.of(
            invalidEntity(entity -> entity.setQuantity(null)), "quantity", MUST_NOT_BE_NULL),
        Arguments.of(
            invalidEntity(entity -> entity.setQuantity(TEST_ZERO_QUANTITY)),
            "quantity",
            MUST_BE_GREATER_THAN_0),
        Arguments.of(
            invalidEntity(entity -> entity.setQuantity(TEST_INVALID_QUANTITY_PRECISION)),
            "quantity",
            MUST_HAVE_UP_TO_11_INTEGER_DIGITS_AND_8_DECIMALS),
        Arguments.of(
            invalidEntity(entity -> entity.setAveragePrice(null)),
            "averagePrice",
            MUST_NOT_BE_NULL),
        Arguments.of(
            invalidEntity(entity -> entity.setAveragePrice(TEST_ZERO_AVERAGE_PRICE)),
            "averagePrice",
            MUST_BE_GREATER_THAN_0),
        Arguments.of(
            invalidEntity(entity -> entity.setAveragePrice(TEST_INVALID_AVERAGE_PRICE_PRECISION)),
            "averagePrice",
            MUST_HAVE_UP_TO_15_INTEGER_DIGITS_AND_4_DECIMALS),
        Arguments.of(
            invalidEntity(entity -> entity.setCurrency(null)), "currency", MUST_NOT_BE_NULL));
  }

  private static AssetCreateRequestDto validCreate() {
    return new AssetCreateRequestDto(
        TEST_ASSET_NAME_APPLE,
        TEST_ASSET_SYMBOL_AAPL,
        AssetTypeEnum.STOCK,
        TEST_ASSET_QUANTITY,
        TEST_ASSET_AVERAGE_PRICE,
        CurrencyCodeEnum.USD);
  }

  public static AssetCreateRequestDto validCreateRequest() {
    return new AssetCreateRequestDto(
        TEST_ASSET_NAME_APPLE,
        TEST_ASSET_SYMBOL_AAPL,
        AssetTypeEnum.STOCK,
        TEST_ASSET_QUANTITY,
        TEST_ASSET_AVERAGE_PRICE,
        CurrencyCodeEnum.USD);
  }

  public static AssetCreateRequestDto createRequestWithNameAndSymbol(String name, String symbol) {
    return new AssetCreateRequestDto(
        name,
        symbol,
        AssetTypeEnum.STOCK,
        TEST_ASSET_QUANTITY,
        TEST_ASSET_AVERAGE_PRICE,
        CurrencyCodeEnum.USD);
  }

  public static PortfolioEntity defaultPortfolioEntity() {
    return EntityBuilder.buildPortfolio(
        TEST_USER_ID,
        TEST_PORTFOLIO_NAME_CORE,
        CurrencyCodeEnum.USD,
        RiskProfileEnum.MEDIUM,
        StrategyTypeEnum.BALANCED,
        InvestmentHorizonEnum.MEDIUM);
  }

  public static AssetEntity defaultAssetEntity(PortfolioEntity portfolio) {
    return EntityBuilder.buildAsset(
        portfolio,
        TEST_ASSET_NAME_APPLE,
        TEST_ASSET_SYMBOL_AAPL,
        AssetTypeEnum.STOCK,
        CurrencyCodeEnum.USD);
  }

  public static AssetResponseDto defaultAssetResponseDto(Long id) {
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    return new AssetResponseDto(
        id,
        CurrencyCodeEnum.USD,
        TEST_ASSET_NAME_APPLE,
        TEST_ASSET_SYMBOL_AAPL,
        AssetTypeEnum.STOCK,
        TEST_ASSET_QUANTITY,
        TEST_ASSET_AVERAGE_PRICE,
        now,
        now);
  }

  private static AssetUpdateRequestDto validUpdate() {
    return new AssetUpdateRequestDto(
        TEST_ASSET_NAME_APPLE,
        TEST_ASSET_SYMBOL_AAPL,
        AssetTypeEnum.STOCK,
        TEST_ASSET_QUANTITY,
        TEST_ASSET_AVERAGE_PRICE,
        CurrencyCodeEnum.USD);
  }

  private static AssetEntity validEntity() {
    return EntityBuilder.buildAsset(
        validPortfolio(),
        TEST_ASSET_NAME_APPLE,
        TEST_ASSET_SYMBOL_AAPL,
        AssetTypeEnum.STOCK,
        CurrencyCodeEnum.USD);
  }

  private static PortfolioEntity validPortfolio() {
    return EntityBuilder.buildPortfolio(
        TEST_USER_ID,
        TEST_PORTFOLIO_NAME_CORE,
        CurrencyCodeEnum.USD,
        RiskProfileEnum.MEDIUM,
        StrategyTypeEnum.BALANCED,
        InvestmentHorizonEnum.MEDIUM);
  }

  private static AssetCreateRequestDto invalidCreate(Consumer<AssetCreateRequestDto> mutator) {
    return mutate(validCreate(), mutator);
  }

  private static AssetUpdateRequestDto invalidUpdate(Consumer<AssetUpdateRequestDto> mutator) {
    return mutate(validUpdate(), mutator);
  }

  private static AssetEntity invalidEntity(Consumer<AssetEntity> mutator) {
    return mutate(validEntity(), mutator);
  }

  private static <T> T mutate(T target, Consumer<T> mutator) {
    mutator.accept(target);
    return target;
  }

  public static AssetImportCandidateDto validImportedAssetCandidate() {
    return new AssetImportCandidateDto(
        TEST_ASSET_NAME_APPLE,
        TEST_ASSET_SYMBOL_AAPL,
        AssetTypeEnum.STOCK,
        TEST_ASSET_RESPONSE_QUANTITY,
        TEST_ASSET_RESPONSE_AVERAGE_PRICE,
        CurrencyCodeEnum.USD);
  }
}
