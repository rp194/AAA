package io.rp194.aaa.accounting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.rp194.aaa.device.DeviceProfile;
import io.rp194.aaa.device.InMemoryDeviceProfileRepository;
import io.rp194.aaa.device.VendorType;
import io.rp194.aaa.session.InMemorySessionStore;
import io.rp194.aaa.session.SessionRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaleSessionReconcilerTest {
  @Test
  void closesDeadStaleSessionsAndEmitsMetricsAndAlerts() {
    InMemorySessionStore sessionStore = new InMemorySessionStore();
    InMemoryDeviceProfileRepository deviceRepo = new InMemoryDeviceProfileRepository();
    InMemoryAccountingLedgerStore ledgerStore = new InMemoryAccountingLedgerStore();
    RecordingMetricsSink metrics = new RecordingMetricsSink();
    RecordingAlerter alerts = new RecordingAlerter();

    Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:10:00Z"), ZoneOffset.UTC);
    deviceRepo.register(new DeviceProfile("tenant-a", "192.0.2.10", null, VendorType.CISCO, "nas-1"));

    sessionStore.upsert(new SessionRecord(
        "tenant-a", "session-1", "user-a", "192.0.2.10", null,
        Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z"),
        111, 222, 60));

    try (AsyncLedgerWriter writer = new AsyncLedgerWriter(ledgerStore, 1)) {
      StaleSessionReconciler reconciler = new StaleSessionReconciler(
          sessionStore,
          deviceRepo,
          session -> NasSessionVerifier.VerificationResult.dead(SessionClosureReason.ROUTER_REBOOT),
          writer,
          metrics,
          alerts,
          clock);

      reconciler.reconcile();
    }

    assertEquals(0, sessionStore.findExpired(clock.instant()).size());
    assertEquals(1, ledgerStore.all().size());
    assertEquals("SESSION_CLOSED:ROUTER_REBOOT", ledgerStore.all().get(0).getReasonCode());
    assertEquals(1, metrics.metrics.size());
    assertEquals(1, alerts.metrics.size());
    assertEquals(VendorType.CISCO, metrics.metrics.get(0).getVendorType());
  }

  private static final class RecordingMetricsSink implements StaleSessionMetricsSink {
    private final List<SessionStaleMetric> metrics = new ArrayList<>();

    @Override
    public void emit(SessionStaleMetric metric) {
      metrics.add(metric);
    }
  }

  private static final class RecordingAlerter implements StaleSessionAlerter {
    private final List<SessionStaleMetric> metrics = new ArrayList<>();

    @Override
    public void alert(SessionStaleMetric metric) {
      metrics.add(metric);
    }
  }
}
