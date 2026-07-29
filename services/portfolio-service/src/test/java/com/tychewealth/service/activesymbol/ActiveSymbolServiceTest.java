package com.tychewealth.service.activesymbol;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.kafka.events.ActiveSymbolChanges;
import com.tychewealth.kafka.publishers.ActiveSymbolChangesEventPublisher;
import com.tychewealth.repository.AssetRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActiveSymbolServiceTest {

  @Mock private AssetRepository assetRepository;
  @Mock private ActiveSymbolStore activeSymbolStore;
  @Mock private ActiveSymbolChangesEventPublisher activeSymbolChangesEventPublisher;

  @InjectMocks private ActiveSymbolService activeSymbolService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(activeSymbolService, "userIdBatchSize", 2);
  }

  @Test
  void validateUserIdBatchSizeRejectsZeroOrNegativeValues() {
    ReflectionTestUtils.setField(activeSymbolService, "userIdBatchSize", 0);

    assertThrows(
        IllegalStateException.class,
        () -> ReflectionTestUtils.invokeMethod(activeSymbolService, "validateUserIdBatchSize"));
  }

  @Test
  void synchronizeSymbolsResolvesSymbolsInBatchesReplacesSnapshotAndPublishesChanges() {
    Set<Long> userIds = new LinkedHashSet<>(List.of(TEST_USER_ID, TEST_OTHER_USER_ID, 126L));
    when(assetRepository.findDistinctSymbolsByUserIds(List.of(TEST_USER_ID, TEST_OTHER_USER_ID)))
        .thenReturn(List.of(TEST_ASSET_SYMBOL_AAPL, TEST_ASSET_SYMBOL_MSFT));
    when(assetRepository.findDistinctSymbolsByUserIds(List.of(126L)))
        .thenReturn(List.of(TEST_ASSET_SYMBOL_AAPL));
    when(activeSymbolStore.findAll()).thenReturn(Set.of(TEST_ASSET_SYMBOL_MSFT, "GOOG"));

    activeSymbolService.synchronizeSymbols(userIds);

    verify(assetRepository).findDistinctSymbolsByUserIds(List.of(TEST_USER_ID, TEST_OTHER_USER_ID));
    verify(assetRepository).findDistinctSymbolsByUserIds(List.of(126L));
    verify(activeSymbolStore).findAll();

    ArgumentCaptor<ActiveSymbolChanges> changesCaptor =
        ArgumentCaptor.forClass(ActiveSymbolChanges.class);
    verify(activeSymbolChangesEventPublisher).publish(changesCaptor.capture());
    assertEquals(Set.of(TEST_ASSET_SYMBOL_AAPL), changesCaptor.getValue().addedSymbols());
    assertEquals(Set.of("GOOG"), changesCaptor.getValue().removedSymbols());

    InOrder inOrder = inOrder(activeSymbolChangesEventPublisher, activeSymbolStore);
    inOrder.verify(activeSymbolChangesEventPublisher).publish(changesCaptor.getValue());
    inOrder.verify(activeSymbolStore).replaceAll(Set.of(TEST_ASSET_SYMBOL_AAPL, TEST_ASSET_SYMBOL_MSFT));
  }

  @Test
  void synchronizeSymbolsReplacesSnapshotAndDoesNotPublishWhenThereAreNoChanges() {
    when(assetRepository.findDistinctSymbolsByUserIds(List.of(TEST_USER_ID)))
        .thenReturn(List.of(TEST_ASSET_SYMBOL_AAPL));
    when(activeSymbolStore.findAll()).thenReturn(Set.of(TEST_ASSET_SYMBOL_AAPL));

    activeSymbolService.synchronizeSymbols(Set.of(TEST_USER_ID));

    verify(activeSymbolStore).findAll();
    verify(activeSymbolStore).replaceAll(Set.of(TEST_ASSET_SYMBOL_AAPL));
    verify(activeSymbolChangesEventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void synchronizeSymbolsDoesNotReplaceSnapshotWhenPublishFails() {
    when(assetRepository.findDistinctSymbolsByUserIds(List.of(TEST_USER_ID)))
        .thenReturn(List.of(TEST_ASSET_SYMBOL_AAPL));
    when(activeSymbolStore.findAll()).thenReturn(Set.of(TEST_ASSET_SYMBOL_MSFT));
    doThrow(new IllegalStateException("broker unavailable"))
        .when(activeSymbolChangesEventPublisher)
        .publish(org.mockito.ArgumentMatchers.any());

    assertThrows(
        IllegalStateException.class, () -> activeSymbolService.synchronizeSymbols(Set.of(TEST_USER_ID)));

    verify(activeSymbolChangesEventPublisher)
        .publish(new ActiveSymbolChanges(Set.of(TEST_ASSET_SYMBOL_AAPL), Set.of(TEST_ASSET_SYMBOL_MSFT)));
    verify(activeSymbolStore, never()).replaceAll(org.mockito.ArgumentMatchers.anySet());
  }

  @Test
  void synchronizeSymbolsPublishesWhenOnlyAddedSymbolsExist() {
    when(assetRepository.findDistinctSymbolsByUserIds(List.of(TEST_USER_ID)))
        .thenReturn(List.of(TEST_ASSET_SYMBOL_AAPL));
    when(activeSymbolStore.findAll()).thenReturn(Set.of());

    activeSymbolService.synchronizeSymbols(Set.of(TEST_USER_ID));

    verify(activeSymbolStore).replaceAll(Set.of(TEST_ASSET_SYMBOL_AAPL));
    ArgumentCaptor<ActiveSymbolChanges> changesCaptor =
        ArgumentCaptor.forClass(ActiveSymbolChanges.class);
    verify(activeSymbolChangesEventPublisher).publish(changesCaptor.capture());
    assertEquals(Set.of(TEST_ASSET_SYMBOL_AAPL), changesCaptor.getValue().addedSymbols());
    assertEquals(Set.of(), changesCaptor.getValue().removedSymbols());
  }

  @Test
  void synchronizeSymbolsPublishesWhenOnlyRemovedSymbolsExist() {
    when(activeSymbolStore.findAll()).thenReturn(Set.of(TEST_ASSET_SYMBOL_AAPL));

    activeSymbolService.synchronizeSymbols(Set.of());

    verify(activeSymbolStore).findAll();
    verify(activeSymbolStore).replaceAll(Set.of());
    ArgumentCaptor<ActiveSymbolChanges> changesCaptor =
        ArgumentCaptor.forClass(ActiveSymbolChanges.class);
    verify(activeSymbolChangesEventPublisher).publish(changesCaptor.capture());
    assertEquals(Set.of(), changesCaptor.getValue().addedSymbols());
    assertEquals(Set.of(TEST_ASSET_SYMBOL_AAPL), changesCaptor.getValue().removedSymbols());
  }
}
