package com.tychewealth.utils.prompts;

public final class AssetImportPromptUtils {

  private static final String SUPPORTED_CURRENCIES =
      "EUR, USD, GBP, CHF, JPY, CNY, ARS, BRL, CLP, COP, DOP, MXN, PEN, PYG, SVC, UYU, VES";
  private static final String TRADE_REPUBLIC_MARKER = "trade republic";

  private AssetImportPromptUtils() {}

  public static String buildAssetImportPrompt(String extractedText) {
    String normalizedText = extractedText == null ? "" : extractedText.trim();
    String providerSpecificGuidance = buildProviderSpecificGuidance(normalizedText);
    return """
        You will receive extracted text from a financial document.
        Your task is to identify all possible assets mentioned in the document and convert them into valid JSON.

        Rules:
        - Return JSON only.
        - The first character of the response must be [.
        - The last character of the response must be ].
        - Do not include markdown fences.
        - Do not add explanations.
        - Do not describe your reasoning.
        - Do not add introductory text such as "Here is the JSON", "Este es el JSON", or similar.
        - Do not add any text before or after the JSON array.
        - If information is missing, use null for fields other than averagePrice.
        - Do not invent values.
        - The root JSON value must be an array.
        - If no asset can be identified with confidence, return [].
        - Never return an empty object such as {}.
        - Every object in the array must contain at least one non-null field.
        - Use only these currency values when present: %s.
        - If the detected currency is not one of the supported values, return null for currency.
        - Use uppercase ISO-style currency codes such as EUR or USD.
        - Each array item must follow this shape:
          {
            "name": "string|null",
            "symbol": "string|null",
            "assetType": "STOCK|ETF|BOND|CRYPTO|CASH|OTHER",
            "quantity": number,
            "averagePrice": number,
            "currency": "string|null"
          }

        Extraction guidance:
        - First reconstruct the document into the most logical reading order before extracting data.
        - Ignore dates, we dont care about them, dont even put them in the reconstruction.
        - If the text looks broken by PDF extraction, mentally regroup fragments that belong to the same row, the same column, or the same table section.
        - Identify the table headers first, then align each following row under those headers before deciding field values.
        - After reconstructing the likely table structure, read it row by row and extract one asset per logical holding row.
        - Keep nearby labels, headers above, and values on the same row together; do not interpret each raw line independently if the PDF extraction split one row into multiple lines.
        - When a holding is split across multiple lines, merge issuer name, instrument description, class, and series text into one logical asset before assigning numbers.
        - Prefer extracting from repeated position rows rather than isolated free text, legal footer text, addresses, disclaimers, or account metadata.
        - Ignore dates unless they are clearly part of an instrument identifier, because statement dates and quote dates are not asset fields here.
        - name: look for instrument names, fund names, company names, bond names, or product descriptions.
        - symbol: look for ticker, symbol, code, or ISIN. If no ticker is present, an ISIN may be used as symbol.
        - assetType: infer from words like stock, share, equity, ETF, fund, bond, treasury, crypto, bitcoin, cash, saldo, efectivo, the unique options that u can choose are this ones for AssetType: [OTHER, CRYPTO, BOND, STOCK, ETF, FOREX, COMMODITY], and just one of them not something like "CRYPTO|ETF".
        - quantity: look for units, shares, participations, titles, nominal, quantity, amount held, or position size.
        - averagePrice: look for average price, unit price, buy price, acquisition price, cost basis per unit, cotization per eur, or market price if it is clearly the only per-unit price shown.
        - averagePrice must never be null.
        - currency: look near price columns or values for symbols/codes such as EUR, USD, GBP, CHF, JPY, CNY, MXN, $, EUR/USD style labels, or account/base currency indicators.
        - When the document uses decimal commas, preserve the numeric magnitude and convert commas to decimal points in JSON numbers instead of dropping the separator.
        - Example: `2,497752` -> `2.497752`.
        - Financial holdings usually appear in repeated rows with a table-like structure. Treat each repeated row pattern as a potential asset entry.
        - If a document contains tabular holdings, create one JSON object per holding row.
        - If a line or block does not follow the same repeated row/table pattern, prefer to ignore it.
        - Read each number together with the words immediately around it and any headers above its column.
        - Column headers and nearby labels are the main clue to decide what a number means.
        - In holdings tables, the leftmost number before the instrument name is often the quantity, especially when the header mentions units, shares, titles, nominal, nominal amount, or position size.
        - In holdings tables, a number under headers such as price, unit price, price per share, price per title, quote, cotizacion, kurs, precio, or average cost is usually averagePrice.
        - In holdings tables, a number under headers such as market value, value, total value, valuation, amount, importe, or balance is usually a total row value and must not be used as quantity.
        - Do not confuse total market value, total value, subtotal, amount invested, proceeds, cash balance, account balance, fees, taxes, gains, losses, or P/L with quantity.
        - Do not confuse quantity with monetary amounts just because both are numeric.
        - If a row has both a unit count and a money amount, use the count for quantity and the per-unit monetary figure for averagePrice.
        - If a row has three numbers that look like quantity, per-unit price, and total valuation, map them in that order and ignore the total valuation for averagePrice.
        - For name, prefer the full human-readable issuer plus instrument description. If the row contains both issuer and product line on separate lines, combine them into one readable name.
        - Ignore isolated numbers that cannot be confidently linked to one asset field by their row pattern, nearby text, or column header.
        - Prefer explicit values from the document over inferred values.
        - Ignore portfolio totals, subtotals, percentages, and performance metrics unless they clearly identify one of the required fields for a specific asset.
        %s

        Output validation:
        - Before answering, verify that your full response is valid JSON.
        - Before answering, verify that the response is only one JSON array and contains no extra text.
        - If you are unsure about a field other than averagePrice, keep that field as null instead of writing explanatory prose.
        - If you cannot identify a valid averagePrice for a candidate row, do not return that asset.

        Extracted text:
        %s
        """
        .formatted(SUPPORTED_CURRENCIES, providerSpecificGuidance, normalizedText);
  }

  private static String buildProviderSpecificGuidance(String extractedText) {
    String normalized = extractedText.toLowerCase();
    if (!normalized.contains(TRADE_REPUBLIC_MARKER)) {
      return "";
    }

    return """

        TRADE REPUBLIC GUIDANCE:
        - IN TRADE REPUBLIC CUSTODY STATEMENTS, HOLDINGS ARE COMMONLY SHOWN IN FOUR LOGICAL COLUMNS: QUANTITY, INSTRUMENT NAME BLOCK, UNIT PRICE, AND TOTAL VALUATION.
        - FOR TRADE REPUBLIC ROWS, THE FIRST NUMERIC VALUE AT THE FAR LEFT SUCH AS `NUMBER TIT.` IS THE QUANTITY.
        - FOR TRADE REPUBLIC ROWS, THE QUOTE DATE SHOWN BELOW THE UNIT PRICE MUST BE IGNORED.
        - FOR TRADE REPUBLIC ROWS, IGNORE DATES, WE DONT CARE ABOUT THEM, DONT EVEN PUT THEM IN THE RECONSTRUCTION.
        - FOR TRADE REPUBLIC ROWS, THIS CANT BE A QUANTITY '42.33', QUANTITYS HAVE WAY MORE DECIMALS, THIS IS COMMONLY A AVERAGE PRICE.
        - FOR TRADE REPUBLIC ROWS, `QUANTITY` MUST LOOK LIKE A TITLES OR UNITS COUNT FROM THE FAR-LEFT COLUMN: AFTER NORMALIZATION IT SHOULD BE A POSITIVE DECIMAL QUANTITY WITH FRACTIONAL PRECISION, NOT A MONETARY AMOUNT, NOT A DATE, AND NOT A TOTAL VALUATION.
        - FOR TRADE REPUBLIC ROWS, NO MONETARY VALUE MAY EVER BE USED AS `QUANTITY`, EVEN IF IT LOOKS NUMERICALLY SIMILAR AFTER NORMALIZATION, A QUANTITY MUST HAVE MUCH MORE DECIMALS, IF ONLY HAS 2 OR 3 DECIMALS ITS A MONETARY VALUE NOT A QUANTITY.
        - FOR TRADE REPUBLIC ROWS, THE COMMA IN THE LEFT QUANTITY VALUE IS A DECIMAL SEPARATOR, NOT A THOUSANDS SEPARATOR.
        - FOR TRADE REPUBLIC ROWS, PRESERVE THE DIGITS EXACTLY AS WRITTEN IN BOTH QUANTITY AND PRICE FIELDS, ONLY CONVERTING DECIMAL COMMAS TO DECIMAL POINTS IN THE JSON OUTPUT.
        - FOR TRADE REPUBLIC ROWS, EXAMPLES OF VALID NUMERIC NORMALIZATION ARE: `2,497752` -> `2.497752`, `42,33` -> `42.33`, `59,78` -> `59.78`, `1.252,46` -> `1252.46`.
        - FOR TRADE REPUBLIC ROWS, THE INSTRUMENT NAME BLOCK MAY SPAN MULTIPLE LINES. COMBINE ISSUER AND PRODUCT DESCRIPTION INTO `NAME`.
        - FOR TRADE REPUBLIC ROWS, IF THE FIRST LINE IS ONLY THE ISSUER AND THE NEXT LINE CONTAINS THE ACTUAL PRODUCT DESCRIPTION, COMBINE BOTH INTO `NAME` INSTEAD OF KEEPING ONLY THE ISSUER.
        - FOR TRADE REPUBLIC ROWS, A LINE BEGINNING WITH `ISIN:` PROVIDES THE BEST FALLBACK SYMBOL WHEN NO TICKER IS EXPLICITLY SHOWN.
        - FOR TRADE REPUBLIC ROWS, `AVERAGEPRICE` MUST NEVER BE NULL , AND  IS THE PER-HOLDING MONETARY VALUE THAT PARTICIPATES IN THE MULTIPLICATION WITH `QUANTITY`: ONE NUMBER IN THE ROW IS THE UNIT PRICE AND, WHEN MULTIPLIED BY `QUANTITY`, IT GIVES THE OTHER MONETARY VALUE SHOWN FOR THAT SAME HOLDING.
        - FOR TRADE REPUBLIC ROWS, ANY PORTFOLIO-LEVEL SUM OR TOTAL MUST BE IGNORED AS `AVERAGEPRICE`, AND IF THE OTHER HOLDING FIELDS ARE IDENTIFIED THEN `AVERAGEPRICE` IS FORBIDDEN FROM BEING NULL; PRESERVE DECIMAL COMMAS CORRECTLY AND DO NOT INFER IT BY CHOOSING THE LARGEST, SMALLEST, OR LAST NUMBER IN THE RAW TEXT.
        - FOR TRADE REPUBLIC ROWS, LABELS LIKE `CUENTA DE VALORES EN ALEMANIA` AND `PAIS DE CUSTODIA` ARE METADATA AND MUST NOT BECOME ASSET FIELDS.
        - FOR TRADE REPUBLIC ROWS, INSTRUMENTS DESCRIBED WITH WORDS LIKE `WISDOMTREE`, `ISHARES`, `PHYSICAL METALS`, `OPEN END`, COMMODITY TRACKER TEXT, EXCHANGE-TRADED PRODUCT WORDING, ETC, ETN, OR METAL-BACKED PRODUCTS SHOULD USUALLY MAP TO `ETF`, NOT `STOCK`.
        - FOR TRADE REPUBLIC ROWS, COMMON COMPANY SHARE WORDING SUCH AS `CORP.`, `INC.`, `REGISTERED SHARES`, OR `CLASS A` USUALLY MEANS `STOCK`, BUT THAT RULE SHOULD NOT OVERRIDE EXPLICIT EXCHANGE-TRADED PRODUCT WORDING LIKE `WISDOMTREE`, `ISHARES`, `OPEN END`, OR `PHYSICAL METALS`.
        """;
  }
}
