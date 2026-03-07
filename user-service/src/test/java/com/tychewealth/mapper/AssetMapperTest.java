package com.tychewealth.mapper;

import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.dto.asset.request.AssetUpdateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.mapper.asset.AssetMapper;
import com.tychewealth.utils.FixtureLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AssetMapperTest {

    private final AssetMapper mapper = Mappers.getMapper(AssetMapper.class);
    private AssetEntity baseEntity;
    private AssetResponseDto responseFixture;
    private AssetCreateRequestDto createFixture;
    private AssetUpdateRequestDto updateFixture;

    @BeforeEach
    void setUp() {
        baseEntity = new AssetEntity();
        baseEntity.setId(10L);
        baseEntity.setSymbol("MSFT");
        baseEntity.setAssetType(AssetTypeEnum.STOCK);
        baseEntity.setQuantity(new BigDecimal("5.25000000"));
        baseEntity.setAveragePrice(new BigDecimal("220.1000"));
        baseEntity.setCurrency(CurrencyCodeEnum.USD);
        baseEntity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        baseEntity.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));

        responseFixture = FixtureLoader.read("/fixtures/asset/asset-response.json", AssetResponseDto.class);
        createFixture = FixtureLoader.read("/fixtures/asset/asset-create-request.json", AssetCreateRequestDto.class);
        updateFixture = FixtureLoader.read("/fixtures/asset/asset-update-request.json", AssetUpdateRequestDto.class);
    }

    @Test
    void toDtoMapsEntityFields() {
        baseEntity.setSymbol("AAPL");
        baseEntity.setAssetType(AssetTypeEnum.STOCK);
        baseEntity.setQuantity(new BigDecimal("10.00000000"));
        baseEntity.setAveragePrice(new BigDecimal("180.4500"));
        baseEntity.setCurrency(CurrencyCodeEnum.USD);
        baseEntity.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        baseEntity.setUpdatedAt(LocalDateTime.of(2026, 3, 7, 11, 30));

        AssetResponseDto dto = mapper.toDto(baseEntity);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("AAPL", dto.getSymbol());
        assertEquals(AssetTypeEnum.STOCK, dto.getAssetType());
        assertEquals(0, new BigDecimal("10.00000000").compareTo(dto.getQuantity()));
        assertEquals(0, new BigDecimal("180.4500").compareTo(dto.getAveragePrice()));
        assertEquals(CurrencyCodeEnum.USD, dto.getCurrency());
        assertEquals(LocalDateTime.of(2026, 3, 1, 10, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 3, 7, 11, 30), dto.getUpdatedAt());
    }

    @Test
    void toEntityMapsDtoFields() {
        AssetEntity entity = mapper.toEntity(responseFixture);

        assertNotNull(entity);
        assertEquals(7L, entity.getId());
        assertEquals("NVDA", entity.getSymbol());
        assertEquals(AssetTypeEnum.STOCK, entity.getAssetType());
        assertEquals(0, new BigDecimal("2.50000000").compareTo(entity.getQuantity()));
        assertEquals(0, new BigDecimal("880.1234").compareTo(entity.getAveragePrice()));
        assertEquals(CurrencyCodeEnum.USD, entity.getCurrency());
        assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0), entity.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 3, 7, 0, 0), entity.getUpdatedAt());
        assertNull(entity.getPortfolio());
    }

    @Test
    void createMapsCreateBodyToEntity() {
        AssetEntity entity = mapper.create(createFixture);

        assertNotNull(entity);
        assertEquals("BTC", entity.getSymbol());
        assertEquals(AssetTypeEnum.CRYPTO, entity.getAssetType());
        assertEquals(0, new BigDecimal("0.12500000").compareTo(entity.getQuantity()));
        assertEquals(0, new BigDecimal("45000.0000").compareTo(entity.getAveragePrice()));
        assertEquals(CurrencyCodeEnum.USD, entity.getCurrency());
        assertNull(entity.getPortfolio());
    }

    @Test
    void updateOnlyChangesNonNullFields() {
        mapper.update(updateFixture, baseEntity);

        assertEquals("ETH", baseEntity.getSymbol());
        assertEquals(AssetTypeEnum.CRYPTO, baseEntity.getAssetType());
        assertEquals(0, new BigDecimal("5.25000000").compareTo(baseEntity.getQuantity()));
        assertEquals(0, new BigDecimal("2500.5000").compareTo(baseEntity.getAveragePrice()));
        assertEquals(CurrencyCodeEnum.USD, baseEntity.getCurrency());
    }
}
