package com.tychewealth.service.helper;

import static com.tychewealth.constants.TestConstants.BLANK_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.OVERSIZED_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.PADDED_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tychewealth.error.exception.InvalidMarketSymbolException;
import com.tychewealth.repository.MarketSymbolRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketSymbolHelperTest {

  @Mock private MarketSymbolRepository marketSymbolRepository;

  @InjectMocks private MarketSymbolHelper marketSymbolHelper;

  @Test
  void applyChangesCreatesUnknownAddedSymbolAsActive() {
    marketSymbolHelper.applyChanges(Set.of(TEST_SYMBOL), Set.of());

    verify(marketSymbolRepository).insertIfAbsentBatch(symbols(TEST_SYMBOL), eq(true), isNull());
    verify(marketSymbolRepository)
        .updateActiveStateBatch(eq(List.of(TEST_SYMBOL)), eq(true), isNull());
  }

  @Test
  void applyChangesCreatesUnknownRemovedSymbolAsInactive() {
    marketSymbolHelper.applyChanges(Set.of(), Set.of(TEST_SYMBOL));

    verify(marketSymbolRepository).insertIfAbsentBatch(symbols(TEST_SYMBOL), eq(false), any());
    verify(marketSymbolRepository)
        .updateActiveStateBatch(eq(List.of(TEST_SYMBOL)), eq(false), any());
  }

  @Test
  void applyChangesNormalizesSymbolBeforeLookingItUp() {
    marketSymbolHelper.applyChanges(Set.of(PADDED_TEST_SYMBOL), Set.of());

    verify(marketSymbolRepository).insertIfAbsentBatch(symbols(TEST_SYMBOL), eq(true), isNull());
  }

  @Test
  void applyChangesDeduplicatesSymbolsAfterNormalization() {
    marketSymbolHelper.applyChanges(Set.of(TEST_SYMBOL, PADDED_TEST_SYMBOL), Set.of());

    verify(marketSymbolRepository).insertIfAbsentBatch(symbols(TEST_SYMBOL), eq(true), isNull());
  }

  @Test
  void applyChangesGivesRemovalPriorityWhenSymbolIsInBothSets() {
    marketSymbolHelper.applyChanges(Set.of(TEST_SYMBOL), Set.of(PADDED_TEST_SYMBOL));

    verify(marketSymbolRepository).insertIfAbsentBatch(symbols(TEST_SYMBOL), eq(false), any());
  }

  @Test
  void applyChangesIgnoresBlankSymbols() {
    marketSymbolHelper.applyChanges(Set.of(BLANK_TEST_SYMBOL), Set.of());

    verifyNoInteractions(marketSymbolRepository);
  }

  @Test
  void applyChangesRejectsSymbolsLongerThanMaximumLength() {
    assertThrows(
        InvalidMarketSymbolException.class,
        () -> marketSymbolHelper.applyChanges(Set.of(OVERSIZED_TEST_SYMBOL), Set.of()));

    verifyNoInteractions(marketSymbolRepository);
  }

  private String[] symbols(String... expectedSymbols) {
    return argThat(actual -> java.util.Arrays.equals(actual, expectedSymbols));
  }
}
