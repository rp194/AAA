package io.rp194.aaa.profile;

import java.util.Objects;

public final class UserProfile {
  private final String tenantId;
  private final String username;
  private final String rateLimit;
  private final String addressList;
  private final String qosPolicy;
  private final int sessionTimeoutSeconds;
  private final int maxConcurrentSessions;

  public UserProfile(String tenantId,
                     String username,
                     String rateLimit,
                     String addressList,
                     String qosPolicy,
                     int sessionTimeoutSeconds,
                     int maxConcurrentSessions) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.username = Objects.requireNonNull(username, "username");
    this.rateLimit = rateLimit;
    this.addressList = addressList;
    this.qosPolicy = qosPolicy;
    this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    this.maxConcurrentSessions = maxConcurrentSessions;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getUsername() {
    return username;
  }

  public String getRateLimit() {
    return rateLimit;
  }

  public String getAddressList() {
    return addressList;
  }

  public String getQosPolicy() {
    return qosPolicy;
  }

  public int getSessionTimeoutSeconds() {
    return sessionTimeoutSeconds;
  }

  public int getMaxConcurrentSessions() {
    return maxConcurrentSessions;
  }
}
