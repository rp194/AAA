package io.rp194.aaa.pod;

import java.util.Objects;

public final class PodAction {
  private final String tenantId;
  private final String username;
  private final String sessionId;
  private final String framedIpAddress;
  private final String nasIp;
  private final String nasPort;
  private final String nasPortId;
  private final String callingStationId;

  public PodAction(String tenantId,
                   String username,
                   String sessionId,
                   String framedIpAddress,
                   String nasIp,
                   String nasPort,
                   String nasPortId,
                   String callingStationId) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.username = Objects.requireNonNull(username, "username");
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.framedIpAddress = framedIpAddress;
    this.nasIp = Objects.requireNonNull(nasIp, "nasIp");
    this.nasPort = nasPort;
    this.nasPortId = nasPortId;
    this.callingStationId = callingStationId;
  }

  public String getTenantId() { return tenantId; }
  public String getUsername() { return username; }
  public String getSessionId() { return sessionId; }
  public String getFramedIpAddress() { return framedIpAddress; }
  public String getNasIp() { return nasIp; }
  public String getNasPort() { return nasPort; }
  public String getNasPortId() { return nasPortId; }
  public String getCallingStationId() { return callingStationId; }
}
