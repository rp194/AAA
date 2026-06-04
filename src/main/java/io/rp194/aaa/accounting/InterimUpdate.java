package io.rp194.aaa.accounting;

import java.time.Instant;
import java.util.Objects;

public final class InterimUpdate {
  private final String tenantId;
  private final String sessionId;
  private final String username;
  private final String nasIp;
  private final String macAddress;
  private final Instant eventTime;
  private final long inputOctets;
  private final long outputOctets;
  private final int interimIntervalSeconds;

  public InterimUpdate(String tenantId,
                       String sessionId,
                       String username,
                       String nasIp,
                       String macAddress,
                       Instant eventTime,
                       long inputOctets,
                       long outputOctets,
                       int interimIntervalSeconds) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.username = Objects.requireNonNull(username, "username");
    this.nasIp = Objects.requireNonNull(nasIp, "nasIp");
    this.macAddress = macAddress;
    this.eventTime = Objects.requireNonNull(eventTime, "eventTime");
    this.inputOctets = inputOctets;
    this.outputOctets = outputOctets;
    this.interimIntervalSeconds = interimIntervalSeconds;
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

  public String getMacAddress() {
    return macAddress;
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

  public int getInterimIntervalSeconds() {
    return interimIntervalSeconds;
  }
}
