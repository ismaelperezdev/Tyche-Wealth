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
import java.time.LocalDateTime;

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
        baseEntity.setId(10L);
        baseEntity.setName("Baseline");
        baseEntity.setDescription("Baseline portfolio description");
        baseEntity.setBaseCurrency(CurrencyCodeEnum.USD);
        baseEntity.setRiskProfile(RiskProfileEnum.MEDIUM);
        baseEntity.setInvestmentHorizon(InvestmentHorizonEnum.MEDIUM);
        baseEntity.setStrategyType(StrategyTypeEnum.BALANCED);
        baseEntity.setMaxRisk(new BigDecimal("0.50"));
        baseEntity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        baseEntity.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));

        responseFixture = FixtureLoader.read("/fixtures/portfolio/portfolio-response.json", PortfolioResponseDto.class);
        createFixture = FixtureLoader.read("/fixtures/portfolio/portfolio-create-request.json", PortfolioCreateRequestDto.class);
        updateFixture = FixtureLoader.read("/fixtures/portfolio/portfolio-update-request.json", PortfolioUpdateRequestDto.class);
    }

    @Test
    void toDtoMapsEntityFields() {
        baseEntity.setName("Core");
        baseEntity.setDescription("Core portfolio");
        baseEntity.setBaseCurrency(CurrencyCodeEnum.EUR);
        baseEntity.setRiskProfile(RiskProfileEnum.LOW);
        baseEntity.setInvestmentHorizon(InvestmentHorizonEnum.SHORT);
        baseEntity.setStrategyType(StrategyTypeEnum.VALUE);
        baseEntity.setMaxRisk(new BigDecimal("0.35"));
        baseEntity.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        baseEntity.setUpdatedAt(LocalDateTime.of(2026, 3, 7, 11, 30));

        PortfolioResponseDto dto = mapper.toDto(baseEntity);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Core", dto.getName());
        assertEquals("Core portfolio", dto.getDescription());
        assertEquals(CurrencyCodeEnum.EUR, dto.getBaseCurrency());
        assertEquals(RiskProfileEnum.LOW, dto.getRiskProfile());
        assertEquals(InvestmentHorizonEnum.SHORT, dto.getInvestmentHorizon());
        assertEquals(StrategyTypeEnum.VALUE, dto.getStrategyType());
        assertEquals(0, new BigDecimal("0.35").compareTo(dto.getMaxRisk()));
        assertEquals(LocalDateTime.of(2026, 3, 1, 10, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 3, 7, 11, 30), dto.getUpdatedAt());
    }

    @Test
    void toEntityMapsDtoFields() {
        PortfolioEntity entity = mapper.toEntity(responseFixture);

        assertNotNull(entity);
        assertEquals(7L, entity.getId());
        assertEquals("Growth", entity.getName());
        assertEquals("Growth portfolio", entity.getDescription());
        assertEquals(CurrencyCodeEnum.USD, entity.getBaseCurrency());
        assertEquals(RiskProfileEnum.HIGH, entity.getRiskProfile());
        assertEquals(InvestmentHorizonEnum.LONG, entity.getInvestmentHorizon());
        assertEquals(StrategyTypeEnum.GROWTH, entity.getStrategyType());
        assertEquals(0, new BigDecimal("0.8").compareTo(entity.getMaxRisk()));
        assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0), entity.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 3, 7, 0, 0), entity.getUpdatedAt());
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
        assertEquals("Baseline portfolio description", baseEntity.getDescription());
        assertEquals(CurrencyCodeEnum.EUR, baseEntity.getBaseCurrency());
        assertEquals(RiskProfileEnum.MEDIUM, baseEntity.getRiskProfile());
        assertEquals(InvestmentHorizonEnum.MEDIUM, baseEntity.getInvestmentHorizon());
        assertEquals(StrategyTypeEnum.DIVIDEND, baseEntity.getStrategyType());
        assertEquals(0, new BigDecimal("0.30").compareTo(baseEntity.getMaxRisk()));
    }
}
