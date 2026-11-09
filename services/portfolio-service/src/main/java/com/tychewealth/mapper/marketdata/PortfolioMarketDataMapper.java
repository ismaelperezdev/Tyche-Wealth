package com.tychewealth.mapper.marketdata;

import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.mapper.GenericMapperConfig;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps MDS price data and portfolio assets to Portfolio market-data response DTOs. */
@Mapper(config = GenericMapperConfig.class)
public interface PortfolioMarketDataMapper {

  @Mapping(target = "assetId", source = "asset.id")
  @Mapping(target = "portfolioId", source = "asset.portfolio.id")
  @Mapping(target = "symbol", source = "asset.symbol")
  @Mapping(target = "quantity", source = "asset.quantity")
  @Mapping(target = "averagePrice", source = "asset.averagePrice")
  @Mapping(target = "marketPrice", source = "latestPrice.price")
  @Mapping(target = "capturedAt", source = "latestPrice.capturedAt")
  AssetMarketPriceDto toLatestPriceDto(AssetEntity asset, LatestMarketPrice latestPrice);

  @Mapping(target = "assetId", source = "asset.id")
  @Mapping(target = "portfolioId", source = "asset.portfolio.id")
  @Mapping(target = "symbol", source = "asset.symbol")
  @Mapping(target = "price", source = "checkpoint.price")
  @Mapping(target = "capturedAt", source = "checkpoint.capturedAt")
  @Mapping(target = "bucketStart", source = "checkpoint.bucketStart")
  AssetPriceCheckpointDto toCheckpointDto(AssetEntity asset, PriceCheckpoint checkpoint);
}
