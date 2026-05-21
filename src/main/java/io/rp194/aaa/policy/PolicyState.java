package io.rp194.aaa.policy;

import java.util.Objects;

public record PolicyState(
    String tenantId,
    String sessionId,
    long totalUsageOctets,
    long lastInputOctets,
    long lastOutputOctets,
    CoaAction lastAction,
    long lastUsageSnapshotOctets) {

  public PolicyState {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(lastAction, "lastAction");
  }

  public static PolicyState empty(String tenantId, String sessionId) {
    return new PolicyState(tenantId, sessionId, 0L, -1L, -1L, CoaAction.NONE, -1L);
  }
}
