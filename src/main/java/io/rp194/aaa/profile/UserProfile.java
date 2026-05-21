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
  private final int bandwidthUpKbps;
  private final int bandwidthDownKbps;
  private final String serviceProfile;

  public UserProfile(String tenantId,
                     String username,
                     String rateLimit,
                     String addressList,
                     String qosPolicy,
                     int sessionTimeoutSeconds,
                     int maxConcurrentSessions,
                     int bandwidthUpKbps,
                     int bandwidthDownKbps,
                     String serviceProfile) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.username = Objects.requireNonNull(username, "username");
    this.rateLimit = rateLimit;
    this.addressList = addressList;
    this.qosPolicy = qosPolicy;
    this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    this.maxConcurrentSessions = maxConcurrentSessions;
    this.bandwidthUpKbps = bandwidthUpKbps;
    this.bandwidthDownKbps = bandwidthDownKbps;
    this.serviceProfile = serviceProfile;
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

  public int getBandwidthUpKbps() {
    return bandwidthUpKbps;
  }

  public int getBandwidthDownKbps() {
    return bandwidthDownKbps;
  }

  public String getServiceProfile() {
    return serviceProfile;
  }
}
