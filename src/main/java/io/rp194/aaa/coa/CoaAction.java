package io.rp194.aaa.coa;

import io.rp194.aaa.device.VendorType;
import java.util.Objects;

public final class CoaAction {
  private final String tenantId;
  private final String username;
  private final String sessionId;
  private final String framedIpAddress;
  private final String nasIp;
  private final VendorType vendorType;
  private final CoaActionType actionType;
  private final String idempotencyKey;

  public CoaAction(String tenantId,
                   String username,
                   String sessionId,
                   String framedIpAddress,
                   String nasIp,
                   VendorType vendorType,
                   CoaActionType actionType,
                   String idempotencyKey) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.username = Objects.requireNonNull(username, "username");
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.framedIpAddress = Objects.requireNonNull(framedIpAddress, "framedIpAddress");
    this.nasIp = Objects.requireNonNull(nasIp, "nasIp");
    this.vendorType = Objects.requireNonNull(vendorType, "vendorType");
    this.actionType = Objects.requireNonNull(actionType, "actionType");
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
  }

  public String getTenantId() { return tenantId; }
  public String getUsername() { return username; }
  public String getSessionId() { return sessionId; }
  public String getFramedIpAddress() { return framedIpAddress; }
  public String getNasIp() { return nasIp; }
  public VendorType getVendorType() { return vendorType; }
  public CoaActionType getActionType() { return actionType; }
  public String getIdempotencyKey() { return idempotencyKey; }
}
