package io.rp194.aaa.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.rp194.aaa.device.InMemoryDeviceProfileRepository;
import io.rp194.aaa.profile.InMemoryUserProfileStore;
import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.AccessRequest;
import io.rp194.aaa.radius.RadiusCode;
import io.rp194.aaa.radius.RadiusPacket;
import io.rp194.aaa.pod.PodAction;
import io.rp194.aaa.pod.PodResult;
import io.rp194.aaa.pod.PodService;
import io.rp194.aaa.session.InMemorySessionStore;
import io.rp194.aaa.session.SessionRecord;
import io.rp194.aaa.vendor.CiscoMapper;
import io.rp194.aaa.vendor.DefaultVendorMapperRegistry;
import io.rp194.aaa.vendor.GenericVendorMapper;
import io.rp194.aaa.vendor.InMemoryTemplateRepository;
import io.rp194.aaa.vendor.MikroTikMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RadiusAccessHandlerTest {
  @Test
  void allowsRoamingWhenNasInMacBindingExceptionList() {
    TestFixture fixture = new TestFixture(new AccessPolicy(2, true, Set.of("roaming-nas"), 0, AccessPolicy.ConcurrencyPolicy.REJECT));
    fixture.seedSession("tenant-a", "old", "user-a", "aa:bb", "2026-05-21T00:00:00Z", 300);

    RadiusPacket response = fixture.handler.handleAccessRequest(request("tenant-a", "user-a", "new", "roaming-nas", "cc:dd"), 1);

    assertEquals(RadiusCode.ACCESS_ACCEPT, response.getCode());
  }

  @Test
  void rejectsOnHardMacBindConflict() {
    TestFixture fixture = new TestFixture(new AccessPolicy(2, true, Set.of(), 0, AccessPolicy.ConcurrencyPolicy.REJECT));
    fixture.seedSession("tenant-a", "old", "user-a", "aa:bb", "2026-05-21T00:00:00Z", 300);

    RadiusPacket response = fixture.handler.handleAccessRequest(request("tenant-a", "user-a", "new", "strict-nas", "cc:dd"), 1);

    assertEquals(RadiusCode.ACCESS_REJECT, response.getCode());
    assertEquals("MAC binding conflict", response.getAttributes().get(0).getValue());
  }

  @Test
  void allowsTakeoverAfterStaleSessionVerification() {
    TestFixture fixture = new TestFixture(new AccessPolicy(1, true, Set.of(), 60, AccessPolicy.ConcurrencyPolicy.REJECT));
    fixture.seedSession("tenant-a", "old", "user-a", "aa:bb", "2026-05-21T00:00:00Z", 60);

    RadiusPacket response = fixture.handler.handleAccessRequest(request("tenant-a", "user-a", "new", "strict-nas", "cc:dd"), 1);

    assertEquals(RadiusCode.ACCESS_ACCEPT, response.getCode());
  }

  @Test
  void rejectsWhenConcurrentSessionLimitStillActive() {
    TestFixture fixture = new TestFixture(new AccessPolicy(2, true, Set.of(), 0, AccessPolicy.ConcurrencyPolicy.REJECT), PodService.NOOP, 1);
    fixture.seedSession("tenant-a", "old", "user-a", "aa:bb", "2026-05-21T00:04:30Z", 300);

    RadiusPacket response = fixture.handler.handleAccessRequest(request("tenant-a", "user-a", "new", "strict-nas", "aa:bb"), 1);

    assertEquals(RadiusCode.ACCESS_REJECT, response.getCode());
    assertEquals("Max concurrent sessions reached for user", response.getAttributes().get(0).getValue());
  }

  @Test
  void triggersPodWhenConcurrencyPolicyEnabled() {
    RecordingPodService podService = new RecordingPodService(PodResult.ACK);
    TestFixture fixture = new TestFixture(new AccessPolicy(2, true, Set.of(), 0, AccessPolicy.ConcurrencyPolicy.POD_OLDEST), podService, 1);
    fixture.seedSession("tenant-a", "old", "user-a", "aa:bb", "2026-05-21T00:00:00Z", 300);

    RadiusPacket response = fixture.handler.handleAccessRequest(request("tenant-a", "user-a", "new", "strict-nas", "aa:bb"), 1);

    assertEquals(RadiusCode.ACCESS_ACCEPT, response.getCode());
    assertEquals(1, podService.actions.size());
    assertEquals("old", podService.actions.get(0).getSessionId());
  }

  private static AccessRequest request(String tenantId, String username, String sessionId, String nasId, String mac) {
    return AccessRequest.builder()
        .tenantId(tenantId)
        .username(username)
        .sessionId(sessionId)
        .nasIp("192.0.2.1")
        .nasIdentifier(nasId)
        .macAddress(mac)
        .interimIntervalSeconds(300)
        .build();
  }

  private static final class TestFixture {
    private final InMemorySessionStore sessions = new InMemorySessionStore();
    private final RadiusAccessHandler handler;

    private TestFixture(AccessPolicy policy) {
      this(policy, PodService.NOOP, 10);
    }

    private TestFixture(AccessPolicy policy, PodService podService, int maxConcurrentSessions) {
      InMemoryUserProfileStore users = new InMemoryUserProfileStore();
      users.upsert(new UserProfile("tenant-a", "user-a", "", "", "", 0, maxConcurrentSessions, 1000, 1000, "default"));
      InMemoryDeviceProfileRepository devices = new InMemoryDeviceProfileRepository();
      List<AccessAuditEvent> events = new ArrayList<>();
      handler = new RadiusAccessHandler(
          devices,
          users,
          new DefaultVendorMapperRegistry(new MikroTikMapper(), new CiscoMapper(), new GenericVendorMapper(), new InMemoryTemplateRepository()),
          sessions,
          Clock.fixed(Instant.parse("2026-05-21T00:05:00Z"), ZoneOffset.UTC),
          policy,
          events::add,
          podService);
    }

    private void seedSession(String tenantId, String sid, String username, String mac, String lastUpdate, int interval) {
      Instant ts = Instant.parse(lastUpdate);
      sessions.upsert(new SessionRecord(tenantId, sid, username, "192.0.2.2", mac, ts, ts, 0L, 0L, interval));
    }
  }

  private static final class RecordingPodService implements PodService {
    private final PodResult result;
    private final List<PodAction> actions = new ArrayList<>();

    private RecordingPodService(PodResult result) {
      this.result = result;
    }

    @Override
    public PodResult disconnect(PodAction action) {
      actions.add(action);
      return result;
    }
  }
}
