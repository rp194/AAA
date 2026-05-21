package io.rp194.aaa.policy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRedisPolicyStateStore implements PolicyStateStore {
  private final Map<String, PolicyState> states = new ConcurrentHashMap<>();

  @Override
  public Optional<PolicyState> find(String tenantId, String sessionId) {
    return Optional.ofNullable(states.get(key(tenantId, sessionId)));
  }

  @Override
  public void upsert(PolicyState state) {
    states.put(key(state.tenantId(), state.sessionId()), state);
  }

  private static String key(String tenantId, String sessionId) {
    return tenantId + ":" + sessionId;
  }
}
