package io.rp194.aaa.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.rp194.aaa.accounting.InterimUpdate;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PolicyServiceTest {
  private final InMemoryRedisPolicyStateStore stateStore = new InMemoryRedisPolicyStateStore();
  private final InMemoryPostgresPolicyEventStore eventStore = new InMemoryPostgresPolicyEventStore();
  private final PolicyService service = new PolicyService(stateStore, eventStore);

  @Test
  void triggersThresholdCrossingActionOnce() {
    PackageModel pkg = new PackageModel(
        1_000,
        PackageModel.ResetCycle.MONTHLY,
        PackageModel.FupProfile.THROTTLE,
        Instant.parse("2099-01-01T00:00:00Z"));

    CoaAction first = service.onInterimUpdate(update(300, 200), pkg);
    CoaAction second = service.onInterimUpdate(update(700, 400), pkg);

    assertEquals(CoaAction.NONE, first);
    assertEquals(CoaAction.DOWNGRADE, second);
    assertEquals(1, eventStore.all().size());
    assertEquals(CoaAction.DOWNGRADE, eventStore.all().get(0).action());
  }

  @Test
  void ignoresDuplicateInterimUpdate() {
    PackageModel pkg = new PackageModel(
        500,
        PackageModel.ResetCycle.MONTHLY,
        PackageModel.FupProfile.NONE,
        Instant.parse("2099-01-01T00:00:00Z"));

    CoaAction first = service.onInterimUpdate(update(400, 200), pkg);
    CoaAction duplicate = service.onInterimUpdate(update(400, 200), pkg);

    assertEquals(CoaAction.CAP_REACHED, first);
    assertEquals(CoaAction.CAP_REACHED, duplicate);
    assertEquals(1, eventStore.all().size());
  }

  @Test
  void handlesOctetWraparoundSafely() {
    PackageModel pkg = new PackageModel(
        2_000,
        PackageModel.ResetCycle.MONTHLY,
        PackageModel.FupProfile.REDIRECT,
        Instant.parse("2099-01-01T00:00:00Z"));

    service.onInterimUpdate(update(4_294_967_000L, 100), pkg);
    CoaAction action = service.onInterimUpdate(update(250, 300), pkg);

    assertEquals(CoaAction.REDIRECT, action);
    assertEquals(1, eventStore.all().size());
    assertEquals(CoaAction.REDIRECT, eventStore.all().get(0).action());
  }

  @Test
  void redirectsWhenPackageExpired() {
    PackageModel pkg = new PackageModel(
        5_000,
        PackageModel.ResetCycle.MONTHLY,
        PackageModel.FupProfile.NONE,
        Instant.now().minusSeconds(60));

    CoaAction action = service.onInterimUpdate(update(100, 100), pkg);

    assertEquals(CoaAction.REDIRECT, action);
    assertEquals(1, eventStore.all().size());
    assertEquals(CoaAction.REDIRECT, eventStore.all().get(0).action());
  }

  @Test
  void triggersFupWhenCrossingThreshold() {
    PackageModel pkg = new PackageModel(
        1_000,
        PackageModel.ResetCycle.MONTHLY,
        PackageModel.FupProfile.THROTTLE,
        Instant.parse("2099-01-01T00:00:00Z"));

    CoaAction first = service.onInterimUpdate(update(500, 490), pkg);
    CoaAction second = service.onInterimUpdate(update(510, 510), pkg);

    assertEquals(CoaAction.NONE, first);
    assertEquals(CoaAction.DOWNGRADE, second);
    assertEquals(1, eventStore.all().size());
    assertEquals(CoaAction.DOWNGRADE, eventStore.all().get(0).action());
  }

  private static InterimUpdate update(long input, long output) {
    return new InterimUpdate(
        "tenant-a",
        "session-a",
        "user-a",
        "192.0.2.1",
        null,
        Instant.parse("2024-01-01T00:00:00Z"),
        input,
        output,
        300);
  }
}
