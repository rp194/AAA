package io.rp194.aaa.server;

import java.util.Map;
import java.util.Objects;

public final class AccessAuditEvent {
  private final String type;
  private final String tenantId;
  private final String username;
  private final Map<String, String> details;

  public AccessAuditEvent(String type, String tenantId, String username, Map<String, String> details) {
    this.type = Objects.requireNonNull(type, "type");
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.username = Objects.requireNonNull(username, "username");
    this.details = Map.copyOf(details);
  }

  public String getType() {
    return type;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getUsername() {
    return username;
  }

  public Map<String, String> getDetails() {
    return details;
  }
}
