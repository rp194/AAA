package io.rp194.aaa.session;

import java.time.Instant;
import java.util.Objects;

public final class SessionRecord {
  private final String tenantId;
  private final String sessionId;
  private final String username;
  private final String nasIp;
  private final String macAddress;
  private final Instant startTime;
  private final Instant lastUpdate;
  private final long inputOctets;
  private final long outputOctets;
  private final int interimIntervalSeconds;

  public SessionRecord(String tenantId,
                       String sessionId,
                       String username,
                       String nasIp,
                       String macAddress,
                       Instant startTime,
                       Instant lastUpdate,
                       long inputOctets,
                       long outputOctets,
                       int interimIntervalSeconds) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.username = Objects.requireNonNull(username, "username");
    this.nasIp = Objects.requireNonNull(nasIp, "nasIp");
    this.macAddress = macAddress;
    this.startTime = Objects.requireNonNull(startTime, "startTime");
    this.lastUpdate = Objects.requireNonNull(lastUpdate, "lastUpdate");
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

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getLastUpdate() {
    return lastUpdate;
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

  public Instant expiresAt() {
    return lastUpdate.plusSeconds(interimIntervalSeconds);
  }

  public SessionRecord withCounters(long inputOctets, long outputOctets, Instant eventTime) {
    return new SessionRecord(tenantId,
        sessionId,
        username,
        nasIp,
        macAddress,
        startTime,
        eventTime,
        inputOctets,
        outputOctets,
        interimIntervalSeconds);
  }
}
