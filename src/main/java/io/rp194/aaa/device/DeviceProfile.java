package io.rp194.aaa.device;

import java.util.Objects;

public final class DeviceProfile {
  private final String tenantId;
  private final String nasIp;
  private final String nasIdentifier;
  private final VendorType vendorType;
  private final String displayName;

  public DeviceProfile(String tenantId, String nasIp, String nasIdentifier, VendorType vendorType, String displayName) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.nasIp = Objects.requireNonNull(nasIp, "nasIp");
    this.nasIdentifier = nasIdentifier;
    this.vendorType = Objects.requireNonNull(vendorType, "vendorType");
    this.displayName = displayName;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getNasIp() {
    return nasIp;
  }

  public String getNasIdentifier() {
    return nasIdentifier;
  }

  public VendorType getVendorType() {
    return vendorType;
  }

  public String getDisplayName() {
    return displayName;
  }
}
