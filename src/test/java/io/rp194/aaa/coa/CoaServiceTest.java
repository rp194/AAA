package io.rp194.aaa.coa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.rp194.aaa.device.VendorType;
import io.rp194.aaa.radius.RadiusPacket;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CoaServiceTest {
  @Test
  void retriesAndPersistsAuditRecordsUntilAck() {
    DefaultVendorEnforcementMapperRegistry registry = new DefaultVendorEnforcementMapperRegistry(
        new MikroTikEnforcementMapper(), new CiscoEnforcementMapper(), actionType -> List.of());
    CoaPacketBuilder builder = new CoaPacketBuilder(registry);
    InMemoryCoaAuditStore auditStore = new InMemoryCoaAuditStore();
    Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    AtomicInteger attempts = new AtomicInteger();
    CoaClient client = (String nasIp, int port, RadiusPacket packet) -> attempts.incrementAndGet() < 3 ? CoaResult.TIMEOUT : CoaResult.ACK;

    CoaService service = new CoaService(builder, client, auditStore, clock, 3);
    CoaAction action = new CoaAction("tenant-a", "user-a", "sess-1", "198.51.100.20", "192.0.2.1", VendorType.CISCO,
        CoaActionType.PLAN_UPGRADE, "idem-1");

    CoaResult result = service.applyPolicyAction(action);

    assertEquals(CoaResult.ACK, result);
    assertEquals(3, auditStore.allAttempts().size());
  }

  @Test
  void returnsDuplicateForIdempotentReplayAfterAck() {
    DefaultVendorEnforcementMapperRegistry registry = new DefaultVendorEnforcementMapperRegistry(
        new MikroTikEnforcementMapper(), new CiscoEnforcementMapper(), actionType -> List.of());
    CoaPacketBuilder builder = new CoaPacketBuilder(registry);
    InMemoryCoaAuditStore auditStore = new InMemoryCoaAuditStore();
    Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    CoaClient client = (String nasIp, int port, RadiusPacket packet) -> CoaResult.ACK;
    CoaService service = new CoaService(builder, client, auditStore, clock, 2);
    CoaAction action = new CoaAction("tenant-a", "user-a", "sess-1", "198.51.100.20", "192.0.2.1", VendorType.CISCO,
        CoaActionType.CAP_REACHED, "idem-dup");

    assertEquals(CoaResult.ACK, service.applyPolicyAction(action));
    assertEquals(CoaResult.DUPLICATE, service.applyPolicyAction(action));
    assertEquals(1, auditStore.allAttempts().size());
  }
}
