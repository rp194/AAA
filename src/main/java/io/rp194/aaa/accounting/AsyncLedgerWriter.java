package io.rp194.aaa.accounting;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class AsyncLedgerWriter implements AutoCloseable {
  private final AccountingLedgerStore ledgerStore;
  private final ExecutorService executor;

  public AsyncLedgerWriter(AccountingLedgerStore ledgerStore, int workerCount) {
    this.ledgerStore = Objects.requireNonNull(ledgerStore, "ledgerStore");
    this.executor = Executors.newFixedThreadPool(workerCount);
  }

  public void enqueue(AccountingUpdate update) {
    executor.execute(() -> ledgerStore.record(update));
  }

  @Override
  public void close() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }
  }
}
