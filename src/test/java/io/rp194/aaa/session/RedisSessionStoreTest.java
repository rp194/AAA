package io.rp194.aaa.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RedisSessionStoreTest {
  @Test
  void tracksExpiredSessionsBasedOnInterimInterval() {
    RedisSessionStore store = new RedisSessionStore();
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
    assertTrue(expired.get(0).getSessionId().equals("session-1"));
  }
}
