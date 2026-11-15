package io.rp194.aaa.accounting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryAccountingLedgerStore implements AccountingLedgerStore {
  private final List<AccountingUpdate> updates = Collections.synchronizedList(new ArrayList<>());

  @Override
  public void record(AccountingUpdate update) {
    updates.add(update);
  }

  @Override
  public List<AccountingUpdate> all() {
    synchronized (updates) {
      return new ArrayList<>(updates);
    }
  }
}
