package io.rp194.aaa.accounting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.rp194.aaa.session.InMemorySessionStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AccountingServiceTest {
  @Test
  void writesLedgerUpdatesAsynchronously() {
    InMemoryAccountingLedgerStore ledgerStore = new InMemoryAccountingLedgerStore();
    InMemorySessionStore sessionStore = new InMemorySessionStore();
    Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    try (AsyncLedgerWriter ledgerWriter = new AsyncLedgerWriter(ledgerStore, 1)) {
      AccountingService service = new AccountingService(sessionStore, ledgerWriter, clock);
      InterimUpdate update = new InterimUpdate(
          "tenant-a",
          "session-1",
          "user-a",
          "192.0.2.10",
          null,
          clock.instant(),
          1234L,
          5678L,
          300);

      service.handleInterimUpdate(update);
    }

    assertEquals(1, ledgerStore.all().size());
  }
}
