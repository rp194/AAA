package io.rp194.aaa.accounting;

import java.time.Instant;
import java.util.Objects;

public final class AccountingUpdate {
  private final String tenantId;
  private final String sessionId;
  private final String username;
  private final String nasIp;
  private final Instant eventTime;
  private final long inputOctets;
  private final long outputOctets;

  public AccountingUpdate(String tenantId,
                          String sessionId,
                          String username,
                          String nasIp,
                          Instant eventTime,
                          long inputOctets,
                          long outputOctets) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.username = Objects.requireNonNull(username, "username");
    this.nasIp = Objects.requireNonNull(nasIp, "nasIp");
    this.eventTime = Objects.requireNonNull(eventTime, "eventTime");
    this.inputOctets = inputOctets;
    this.outputOctets = outputOctets;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getUsername() {
    return username;
  }

  public String getNasIp() {
    return nasIp;
  }

  public Instant getEventTime() {
    return eventTime;
  }

  public long getInputOctets() {
    return inputOctets;
  }

  public long getOutputOctets() {
    return outputOctets;
  }
}
