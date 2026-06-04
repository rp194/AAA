package io.rp194.aaa.accounting;

import io.rp194.aaa.policy.PackageModel;
import io.rp194.aaa.policy.PolicyService;
import io.rp194.aaa.session.SessionRecord;
import io.rp194.aaa.session.SessionStore;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class AccountingService {
  private final SessionStore sessionStore;
  private final AsyncLedgerWriter ledgerWriter;
  private final Clock clock;
  private final PolicyService policyService;
  private final PackageModel packageModel;

  public AccountingService(SessionStore sessionStore, AsyncLedgerWriter ledgerWriter, Clock clock) {
    this(sessionStore, ledgerWriter, clock, null, null);
  }

  public AccountingService(SessionStore sessionStore,
                           AsyncLedgerWriter ledgerWriter,
                           Clock clock,
                           PolicyService policyService,
                           PackageModel packageModel) {
    this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    this.ledgerWriter = Objects.requireNonNull(ledgerWriter, "ledgerWriter");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.policyService = policyService;
    this.packageModel = packageModel;
  }

  public void handleInterimUpdate(InterimUpdate update) {
    Instant eventTime = update.getEventTime() != null ? update.getEventTime() : clock.instant();
    Optional<SessionRecord> existing = sessionStore.find(update.getTenantId(), update.getSessionId());
    SessionRecord record = existing
        .map(session -> session.withCounters(update.getInputOctets(), update.getOutputOctets(), eventTime))
        .orElseGet(() -> new SessionRecord(
            update.getTenantId(),
            update.getSessionId(),
            update.getUsername(),
            update.getNasIp(),
            update.getMacAddress(),
            eventTime,
            eventTime,
            update.getInputOctets(),
            update.getOutputOctets(),
            update.getInterimIntervalSeconds()));

    sessionStore.upsert(record);

    if (policyService != null && packageModel != null) {
      policyService.onInterimUpdate(update, packageModel);
    }

    ledgerWriter.enqueue(new AccountingUpdate(
        update.getTenantId(),
        update.getSessionId(),
        update.getUsername(),
        update.getNasIp(),
        eventTime,
        update.getInputOctets(),
        update.getOutputOctets()));
  }
}
