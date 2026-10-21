package com.tychewealth.listing;

/** Shared values for listing-support tests and their integration fixture. */
public final class ListingTestConstants {

  public static final String ASSET_ENDPOINT = "/integration/assets";
  public static final String ASSET_ENTITY_NAME = "ListingAsset";

  public static final String SYMBOL_FIELD = "symbol";
  public static final String TICKER_FIELD = "ticker";
  public static final String NAME_FIELD = "name";
  public static final String CATEGORY_FIELD = "category";
  public static final String DETAILS_CATEGORY_FIELD = "details.category";
  public static final String QUANTITY_FIELD = "quantity";
  public static final String ACQUIRED_ON_FIELD = "acquiredOn";
  public static final String STATUS_FIELD = "status";

  public static final String SORT_PARAM = "sort";
  public static final String FIELDS_PARAM = "fields";
  public static final String OFFSET_PARAM = "offset";
  public static final String LIMIT_PARAM = "limit";
  public static final String PAGE_PARAM = "page";

  public static final String RANGE_HEADER = "Range";
  public static final String CONTENT_RANGE_HEADER = "Content-Range";
  public static final String TOTAL_COUNT_HEADER = "X-Total-Count";
  public static final String LINK_HEADER = "Link";

  public static final String ASSET_AAPL = "AAPL";
  public static final String ASSET_MSFT = "MSFT";
  public static final String ASSET_XOM = "XOM";
  public static final String ASSET_BABA = "BABA";
  public static final String NAME_APPLE = "Apple";
  public static final String NAME_MICROSOFT = "Microsoft";
  public static final String NAME_EXXON = "Exxon";
  public static final String NAME_ALIBABA = "Alibaba";
  public static final String CATEGORY_TECHNOLOGY = "Technology";
  public static final String CATEGORY_ENERGY = "Energy";
  public static final String DATE_AAPL_ACQUIRED = "2023-01-10";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_INACTIVE = "INACTIVE";
  public static final String NULL_TOKEN = "null";

  private ListingTestConstants() {}
}
