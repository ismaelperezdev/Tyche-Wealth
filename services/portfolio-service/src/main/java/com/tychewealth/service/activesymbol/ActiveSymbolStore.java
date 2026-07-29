package com.tychewealth.service.activesymbol;

import java.util.Set;

public interface ActiveSymbolStore {

  void replaceAll(Set<String> symbols);

  Set<String> findAll();
}
