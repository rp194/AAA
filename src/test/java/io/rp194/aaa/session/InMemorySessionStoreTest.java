package io.rp194.aaa.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemorySessionStoreTest {
  @Test
  void tracksExpiredSessionsBasedOnInterimInterval() {
    InMemorySessionStore store = new InMemorySessionStore();
    Instant start = Instant.parse("2024-01-01T00:00:00Z");
    SessionRecord record = new SessionRecord(
        "tenant-a",
        "session-1",
        "user-a",
        "192.0.2.10",
        null,
        start,
        start,
        0L,
        0L,
        60);

    store.upsert(record);

    List<SessionRecord> expired = store.findExpired(start.plusSeconds(61));

    assertEquals(1, expired.size());
    assertEquals("session-1", expired.get(0).getSessionId());
  }

  @Test
  void isolatesSessionsAcrossTenants() {
    InMemorySessionStore store = new InMemorySessionStore();
    Instant start = Instant.parse("2024-01-01T00:00:00Z");
    store.upsert(new SessionRecord("tenant-a", "session-1", "user-a", "192.0.2.10", null, start, start, 0L, 0L, 60));
    store.upsert(new SessionRecord("tenant-b", "session-1", "user-a", "192.0.2.11", null, start, start, 0L, 0L, 60));

    assertEquals(1, store.findByTenant("tenant-a").size());
    assertEquals(1, store.findByTenant("tenant-b").size());
    assertEquals(1, store.findByTenantAndUsername("tenant-a", "user-a").size());
    assertEquals(1, store.findByTenantAndUsername("tenant-b", "user-a").size());
    assertEquals("192.0.2.10", store.find("tenant-a", "session-1").orElseThrow().getNasIp());
    assertEquals("192.0.2.11", store.find("tenant-b", "session-1").orElseThrow().getNasIp());
  }
}
