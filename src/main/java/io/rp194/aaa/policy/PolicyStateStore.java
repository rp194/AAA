package io.rp194.aaa.policy;

import java.util.Optional;

public interface PolicyStateStore {
  Optional<PolicyState> find(String tenantId, String sessionId);
  void upsert(PolicyState state);
}
