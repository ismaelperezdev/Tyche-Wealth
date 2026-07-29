package com.tychewealth.service.activesymbol;

import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.SYSTEM;

import com.tychewealth.kafka.events.ActiveSymbolChanges;
import com.tychewealth.kafka.publishers.ActiveSymbolChangesEventPublisher;
import com.tychewealth.repository.AssetRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveSymbolService {

  private static final String ACTIVE_SYMBOL_ACTION = "[active-symbol-sync]";

  private final AssetRepository assetRepository;
  private final ActiveSymbolStore activeSymbolStore;
  private final ActiveSymbolChangesEventPublisher activeSymbolChangesEventPublisher;

  @Value("${app.active-symbol.user-id-batch-size:600}")
  private int userIdBatchSize;

  @PostConstruct
  void validateUserIdBatchSize() {
    if (userIdBatchSize <= 0) {
      throw new IllegalStateException(
          "app.active-symbol.user-id-batch-size must be greater than zero");
    }
  }

  public void synchronizeSymbols(Set<Long> userIds) {
    log.info(REQUEST_START, SYSTEM, ACTIVE_SYMBOL_ACTION);

    Set<String> currentSymbols = resolveActiveSymbols(userIds);
    Set<String> previousSymbols = activeSymbolStore.findAll();
    ActiveSymbolChanges changes = resolveSymbolChanges(previousSymbols, currentSymbols);

    if (!changes.addedSymbols().isEmpty() || !changes.removedSymbols().isEmpty()) {
      activeSymbolChangesEventPublisher.publish(changes);
    }

    activeSymbolStore.replaceAll(currentSymbols);
    log.info(
        REQUEST_SUCCESS + " activeSymbols={}", SYSTEM, ACTIVE_SYMBOL_ACTION, currentSymbols.size());
  }

  private Set<String> resolveActiveSymbols(Set<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Set.of();
    }

    List<Long> userIdList = new ArrayList<>(userIds);
    Set<String> symbols = new LinkedHashSet<>();

    for (int batchStart = 0; batchStart < userIdList.size(); batchStart += userIdBatchSize) {
      int batchEnd = Math.min(batchStart + userIdBatchSize, userIdList.size());
      List<Long> userIdBatch = userIdList.subList(batchStart, batchEnd);
      symbols.addAll(assetRepository.findDistinctSymbolsByUserIds(userIdBatch));
    }

    return Set.copyOf(symbols);
  }

  private ActiveSymbolChanges resolveSymbolChanges(
      Set<String> previousSymbols, Set<String> currentSymbols) {
    Set<String> safePreviousSymbols = previousSymbols == null ? Set.of() : previousSymbols;
    Set<String> safeCurrentSymbols = currentSymbols == null ? Set.of() : currentSymbols;

    Set<String> addedSymbols = new LinkedHashSet<>(safeCurrentSymbols);
    addedSymbols.removeAll(safePreviousSymbols);

    Set<String> removedSymbols = new LinkedHashSet<>(safePreviousSymbols);
    removedSymbols.removeAll(safeCurrentSymbols);

    return new ActiveSymbolChanges(Set.copyOf(addedSymbols), Set.copyOf(removedSymbols));
  }
}
