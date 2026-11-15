package io.rp194.aaa.accounting;

import java.util.List;

public interface AccountingLedgerStore {
  void record(AccountingUpdate update);

  List<AccountingUpdate> all();
}
