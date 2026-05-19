package com.tychewealth.constants;

public final class PersistenceConstants {

  public static final String PORTFOLIO_UNIQUE_CONSTRAINT = "uq_portfolio_user_id_name";
  public static final String ASSET_SYMBOL_UNIQUE_CONSTRAINT = "uq_asset_portfolio_id_symbol";

  private PersistenceConstants() {}
}
