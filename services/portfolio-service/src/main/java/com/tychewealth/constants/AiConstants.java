package com.tychewealth.constants;

import com.tychewealth.enums.AssetTypeEnum;
import java.util.List;
import java.util.regex.Pattern;

public final class AiConstants {

  public static final Pattern HOLDING_START_PATTERN =
      Pattern.compile(
          "^(?<quantity>\\d[\\d.,]*)\\s+(?<unit>[\\p{L}]{1,12}\\.?)(?:\\s+(?<name>.+))?$");
  public static final Pattern ISIN_PATTERN = Pattern.compile("\\b[A-Z]{2}[A-Z0-9]{10}\\b");
  public static final Pattern DATE_PATTERN = Pattern.compile("^\\d{2}[./-]\\d{2}[./-]\\d{4}$");
  public static final Pattern NUMERIC_VALUE_PATTERN =
      Pattern.compile("^(?:[A-Z]{3}\\s*)?\\d[\\d.,]*(?:\\s*[A-Z]{3})?$");
  public static final List<AssetTypeEnum> INFERRED_ASSET_TYPE_ORDER =
      List.of(AssetTypeEnum.CRYPTO, AssetTypeEnum.BOND, AssetTypeEnum.ETF, AssetTypeEnum.STOCK);
}
