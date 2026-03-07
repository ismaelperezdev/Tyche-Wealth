package com.tychewealth.mapper;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import com.tychewealth.mapper.portfolio.PortfolioMapper;
import com.tychewealth.utils.FixtureLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PortfolioMapperTest {

    private final PortfolioMapper mapper = Mappers.getMapper(PortfolioMapper.class);
    private PortfolioEntity baseEntity;
    private PortfolioResponseDto responseFixture;
    private PortfolioCreateRequestDto createFixture;
    private PortfolioUpdateRequestDto updateFixture;

    @BeforeEach
    void setUp() {
        baseEntity = new PortfolioEntity();
        baseEntity.setName("Before");
        baseEntity.setDescription("Before description");
        baseEntity.setBaseCurrency(CurrencyCodeEnum.USD);
        baseEntity.setRiskProfile(RiskProfileEnum.MEDIUM);
        baseEntity.setInvestmentHorizon(InvestmentHorizonEnum.MEDIUM);
        baseEntity.setStrategyType(StrategyTypeEnum.BALANCED);
        baseEntity.setMaxRisk(new BigDecimal("0.50"));

        responseFixture = FixtureLoader.read("/fixtures/portfolio/portfolio-response.json", PortfolioResponseDto.class);
        createFixture = FixtureLoader.read("/fixtures/portfolio/portfolio-create-request.json", PortfolioCreateRequestDto.class);
        updateFixture = FixtureLoader.read("/fixtures/portfolio/portfolio-update-request.json", PortfolioUpdateRequestDto.class);
    }

    @Test
    void toDtoMapsEntityFields() {
        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(10L);
        entity.setName("Core");
        entity.setDescription("Core portfolio");

        PortfolioResponseDto dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Core", dto.getName());
        assertEquals("Core portfolio", dto.getDescription());
        assertNull(dto.getCurrency());
        assertNull(dto.getMaxRisk());
    }

    @Test
    void toEntityMapsDtoFields() {
        PortfolioEntity entity = mapper.toEntity(responseFixture);

        assertNotNull(entity);
        assertEquals(7L, entity.getId());
        assertEquals("Growth", entity.getName());
        assertEquals("Growth portfolio", entity.getDescription());
        assertNull(entity.getBaseCurrency());
        assertEquals(0, new BigDecimal("0.8").compareTo(entity.getMaxRisk()));
    }

    @Test
    void createMapsCreateBodyToEntity() {
        PortfolioEntity entity = mapper.create(createFixture);

        assertNotNull(entity);
        assertEquals("Income", entity.getName());
        assertEquals("Income portfolio", entity.getDescription());
        assertEquals(CurrencyCodeEnum.EUR, entity.getBaseCurrency());
        assertEquals(RiskProfileEnum.LOW, entity.getRiskProfile());
        assertEquals(InvestmentHorizonEnum.LONG, entity.getInvestmentHorizon());
        assertEquals(StrategyTypeEnum.INCOME, entity.getStrategyType());
        assertEquals(0, new BigDecimal("0.25").compareTo(entity.getMaxRisk()));
        assertNull(entity.getUser());
    }

    @Test
    void updateOnlyChangesNonNullFields() {
        mapper.update(updateFixture, baseEntity);

        assertEquals("After", baseEntity.getName());
        assertEquals("Before description", baseEntity.getDescription());
        assertEquals(CurrencyCodeEnum.EUR, baseEntity.getBaseCurrency());
        assertEquals(RiskProfileEnum.MEDIUM, baseEntity.getRiskProfile());
        assertEquals(InvestmentHorizonEnum.MEDIUM, baseEntity.getInvestmentHorizon());
        assertEquals(StrategyTypeEnum.DIVIDEND, baseEntity.getStrategyType());
        assertEquals(0, new BigDecimal("0.30").compareTo(baseEntity.getMaxRisk()));
    }
}
