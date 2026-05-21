package io.rp194.aaa.accounting;

import io.rp194.aaa.device.VendorType;
import java.time.Instant;
import java.util.Objects;

public final class SessionStaleMetric {
  private final String tenantId;
  private final String nasIp;
  private final VendorType vendorType;
  private final long staleCount;
  private final Instant observedAt;

  public SessionStaleMetric(String tenantId, String nasIp, VendorType vendorType, long staleCount, Instant observedAt) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.nasIp = Objects.requireNonNull(nasIp, "nasIp");
    this.vendorType = Objects.requireNonNull(vendorType, "vendorType");
    this.staleCount = staleCount;
    this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
  }

  public String getTenantId() { return tenantId; }
  public String getNasIp() { return nasIp; }
  public VendorType getVendorType() { return vendorType; }
  public long getStaleCount() { return staleCount; }
  public Instant getObservedAt() { return observedAt; }
}
