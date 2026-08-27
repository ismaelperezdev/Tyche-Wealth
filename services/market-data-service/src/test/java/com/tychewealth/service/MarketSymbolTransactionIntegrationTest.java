package com.tychewealth.service;

import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.repository.MarketSymbolRepository;
import com.tychewealth.service.helper.MarketSymbolHelper;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:market-symbol-transaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.liquibase.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MarketSymbolService.class, MarketSymbolHelper.class})
class MarketSymbolTransactionIntegrationTest {

  @Autowired private MarketSymbolService marketSymbolService;

  @Autowired private MarketSymbolRepository marketSymbolRepository;

  @SpyBean private MarketSymbolHelper helperSpy;

  @BeforeEach
  void setUp() {
    marketSymbolRepository.deleteAll();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void rollsBackAllSymbolsWhenProcessingFailsAfterDeltaPersistence() {
    doAnswer(
            invocation -> {
              MarketSymbolEntity firstSymbol = new MarketSymbolEntity();
              firstSymbol.setSymbol(TEST_SYMBOL);
              firstSymbol.setActive(true);
              MarketSymbolEntity secondSymbol = new MarketSymbolEntity();
              secondSymbol.setSymbol("MSFT");
              secondSymbol.setActive(true);
              marketSymbolRepository.saveAll(Set.of(firstSymbol, secondSymbol));
              throw new IllegalStateException("Simulated processing failure");
            })
        .when(helperSpy)
        .applyChanges(any(), any());

    Set<String> symbols = new LinkedHashSet<>(Set.of(TEST_SYMBOL, "MSFT"));

    assertThrows(
        IllegalStateException.class, () -> marketSymbolService.applyChanges(symbols, Set.of()));

    assertEquals(0, marketSymbolRepository.count());
  }
}
