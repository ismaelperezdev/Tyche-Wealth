package com.tychewealth.service.activesymbol;

import java.util.Set;

/**
 * Storage abstraction for the distributed snapshot of symbols used by active users.
 *
 * <p>Allows the synchronization service to replace the complete symbol set and read the previous
 * snapshot without coupling the use case to a specific backing store.
 */
public interface ActiveSymbolStore {

  void replaceAll(Set<String> symbols);

  Set<String> findAll();
}
