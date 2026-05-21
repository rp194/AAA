package io.rp194.aaa.accounting;

import io.rp194.aaa.device.DeviceProfileRepository;
import io.rp194.aaa.device.VendorType;
import io.rp194.aaa.session.SessionRecord;
import io.rp194.aaa.session.SessionStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StaleSessionReconciler {
  private final SessionStore sessionStore;
  private final DeviceProfileRepository deviceProfileRepository;
  private final NasSessionVerifier nasSessionVerifier;
  private final AsyncLedgerWriter ledgerWriter;
  private final StaleSessionMetricsSink metricsSink;
  private final StaleSessionAlerter alerter;
  private final Clock clock;

  public StaleSessionReconciler(SessionStore sessionStore,
                                DeviceProfileRepository deviceProfileRepository,
                                NasSessionVerifier nasSessionVerifier,
                                AsyncLedgerWriter ledgerWriter,
                                StaleSessionMetricsSink metricsSink,
                                StaleSessionAlerter alerter,
                                Clock clock) {
    this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    this.deviceProfileRepository = Objects.requireNonNull(deviceProfileRepository, "deviceProfileRepository");
    this.nasSessionVerifier = Objects.requireNonNull(nasSessionVerifier, "nasSessionVerifier");
    this.ledgerWriter = Objects.requireNonNull(ledgerWriter, "ledgerWriter");
    this.metricsSink = Objects.requireNonNull(metricsSink, "metricsSink");
    this.alerter = Objects.requireNonNull(alerter, "alerter");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public void reconcile() {
    Instant now = clock.instant();
    List<SessionRecord> sessions = sessionStore.findExpired(now);
    Map<String, Long> staleByNasAndVendor = new HashMap<>();

    for (SessionRecord session : sessions) {
      Duration age = Duration.between(session.getLastUpdate(), now);
      long staleThresholdSeconds = 2L * session.getInterimIntervalSeconds();
      if (age.getSeconds() < staleThresholdSeconds) {
        continue;
      }

      VendorType vendorType = deviceProfileRepository
          .findByNas(session.getTenantId(), session.getNasIp(), null)
          .map(profile -> profile.getVendorType())
          .orElse(VendorType.GENERIC);

      String metricKey = session.getTenantId() + "::" + session.getNasIp() + "::" + vendorType;
      staleByNasAndVendor.merge(metricKey, 1L, Long::sum);

      NasSessionVerifier.VerificationResult verification = nasSessionVerifier.verify(session);
      if (!verification.isAlive()) {
        sessionStore.remove(session.getTenantId(), session.getSessionId());
        ledgerWriter.enqueue(new AccountingUpdate(
            session.getTenantId(),
            session.getSessionId(),
            session.getUsername(),
            session.getNasIp(),
            now,
            session.getInputOctets(),
            session.getOutputOctets(),
            "SESSION_CLOSED:" + normalizeReason(verification.getReason())));
      }
    }

    for (Map.Entry<String, Long> entry : staleByNasAndVendor.entrySet()) {
      String[] parts = entry.getKey().split("::");
      SessionStaleMetric metric = new SessionStaleMetric(
          parts[0],
          parts[1],
          VendorType.valueOf(parts[2]),
          entry.getValue(),
          now);
      metricsSink.emit(metric);
      alerter.alert(metric);
    }
  }

  private static String normalizeReason(SessionClosureReason reason) {
    return reason != null ? reason.name() : SessionClosureReason.UNKNOWN_SESSION.name();
  }
}
