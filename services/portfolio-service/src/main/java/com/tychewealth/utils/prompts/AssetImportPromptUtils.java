package com.tychewealth.utils.prompts;

public final class AssetImportPromptUtils {

  private AssetImportPromptUtils() {}

  public static String buildAssetImportPrompt(String extractedText) {
    String normalizedText = extractedText == null ? "" : extractedText.trim();
    return """
        You will receive extracted text from a financial document.
        Your task is to identify all possible assets mentioned in the document and convert them into valid JSON.

        Rules:
        - Return JSON only.
        - Do not include markdown fences.
        - Do not add explanations.
        - If information is missing, use null.
        - Do not invent values.
        - The root JSON value must be an array.
        - Each array item must follow this shape:
          {
            "name": "string|null", // Human-readable asset name, for example Apple Inc. or Vanguard FTSE All-World UCITS ETF
            "symbol": "string|null", // Ticker or trading symbol of the asset, for example AAPL or VWCE
            "assetType": "STOCK|ETF|BOND|CRYPTO|CASH|OTHER|null", // Best matching asset category
            "quantity": number|null, // Number of units, shares or coins owned or purchased
            "averagePrice": number|null, // Average or unit purchase price per asset unit
            "currency": "string|null" // Currency code associated with the asset price, for example EUR or USD
          }

        Extracted text:
        %s
        """
        .formatted(normalizedText);
  }
}
