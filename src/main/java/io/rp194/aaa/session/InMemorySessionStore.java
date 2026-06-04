package io.rp194.aaa.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySessionStore implements SessionStore {
  private final Map<SessionKey, SessionRecord> sessions = new ConcurrentHashMap<>();

  @Override
  public void upsert(SessionRecord record) {
    sessions.put(new SessionKey(record.getTenantId(), record.getSessionId()), record);
  }

  @Override
  public Optional<SessionRecord> find(String tenantId, String sessionId) {
    return Optional.ofNullable(sessions.get(new SessionKey(tenantId, sessionId)));
  }

  @Override
  public List<SessionRecord> findByTenantAndUsername(String tenantId, String username) {
    List<SessionRecord> matches = new ArrayList<>();
    for (SessionRecord record : sessions.values()) {
      if (record.getTenantId().equals(tenantId) && record.getUsername().equals(username)) {
        matches.add(record);
      }
    }
    return matches;
  }

  @Override
  public List<SessionRecord> findByTenant(String tenantId) {
    List<SessionRecord> matches = new ArrayList<>();
    for (SessionRecord record : sessions.values()) {
      if (record.getTenantId().equals(tenantId)) {
        matches.add(record);
      }
    }
    return matches;
  }

  @Override
  public List<SessionRecord> findExpired(Instant now) {
    List<SessionRecord> expired = new ArrayList<>();
    for (SessionRecord record : sessions.values()) {
      if (!record.expiresAt().isAfter(now)) {
        expired.add(record);
      }
    }
    return expired;
  }

  @Override
  public void remove(String tenantId, String sessionId) {
    sessions.remove(new SessionKey(tenantId, sessionId));
  }
}
