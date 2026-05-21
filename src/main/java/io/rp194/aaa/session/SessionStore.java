package io.rp194.aaa.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SessionStore {
  void upsert(SessionRecord record);

  Optional<SessionRecord> find(String tenantId, String sessionId);

  List<SessionRecord> findExpired(Instant now);

  void remove(String tenantId, String sessionId);
}
