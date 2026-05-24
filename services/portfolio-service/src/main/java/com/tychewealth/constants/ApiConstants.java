package com.tychewealth.constants;

import org.springframework.http.MediaType;

public final class ApiConstants {

  public static final String URL_FOLDER = "/tyche-wealth/portfolio-service";
  public static final String VERSION_1 = "/v1";
  public static final String URL_FOLDER_V1 = URL_FOLDER + VERSION_1;
  public static final String PORTFOLIO_ID_PATH = "portfolioId";
  public static final String ASSET_ID_PATH = "assetId";
  public static final String IMPORT_ID_PATH = "importId";
  public static final String PAGE_PARAM = "page";
  public static final String LIMIT_PARAM = "limit";
  public static final String DEFAULT_PAGE = "0";
  public static final String DEFAULT_ASSET_LIST_LIMIT = "10";
  public static final int MAX_ASSET_LIST_LIMIT = 10;
  public static final String X_TOTAL_COUNT_HEADER = "X-Total-Count";
  public static final String X_PAGE_HEADER = "X-Page";
  public static final String X_LIMIT_HEADER = "X-Limit";
  public static final String X_HAS_NEXT_HEADER = "X-Has-Next";
  public static final String PORTFOLIO_BASE_URL = URL_FOLDER_V1 + "/portfolio";
  public static final String ASSET_BASE_URL = URL_FOLDER_V1 + "/assets";
  public static final String PORTFOLIO_ASSET_BASE_URL = URL_FOLDER_V1 + "/me/{portfolioId}/assets";
  public static final String PORTFOLIO_ASSET_BY_ID_URL = PORTFOLIO_ASSET_BASE_URL + "/{assetId}";
  public static final String ASSET_IMPORT_URL = ASSET_BASE_URL + "/import";
  public static final String ASSET_IMPORT_BY_ID_URL =
      ASSET_IMPORT_URL + "/{" + IMPORT_ID_PATH + "}";
  public static final String PORTFOLIO_ASSET_BATCH_URL = PORTFOLIO_ASSET_BASE_URL + "/batch";

  public static final String REQUEST_PRODUCES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
  public static final String REQUEST_CONSUMES = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
  public static final String MULTIPART_FORM_DATA = MediaType.MULTIPART_FORM_DATA_VALUE;

  private ApiConstants() {}
}
