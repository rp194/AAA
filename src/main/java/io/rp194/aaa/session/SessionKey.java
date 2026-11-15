package io.rp194.aaa.session;

import java.util.Objects;

public final class SessionKey {
  private final String tenantId;
  private final String sessionId;

  public SessionKey(String tenantId, String sessionId) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getSessionId() {
    return sessionId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SessionKey)) {
      return false;
    }
    SessionKey that = (SessionKey) other;
    return tenantId.equals(that.tenantId) && sessionId.equals(that.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, sessionId);
  }
}
