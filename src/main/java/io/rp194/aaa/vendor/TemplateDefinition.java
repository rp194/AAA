package io.rp194.aaa.vendor;

import io.rp194.aaa.device.VendorType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TemplateDefinition(
    String tenantId,
    VendorType vendorType,
    String deviceKey,
    String version,
    int rolloutPercent,
    Instant effectiveFrom,
    List<TemplateAttribute> attributes) {

  public TemplateDefinition {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(vendorType, "vendorType");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(effectiveFrom, "effectiveFrom");
    attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
  }
}
