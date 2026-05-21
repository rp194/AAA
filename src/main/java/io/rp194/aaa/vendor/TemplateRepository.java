package io.rp194.aaa.vendor;

import io.rp194.aaa.device.VendorType;
import java.time.Instant;
import java.util.Optional;

public interface TemplateRepository {
  Optional<TemplateDefinition> findTemplate(String tenantId, VendorType vendorType, String deviceKey, String subjectKey, Instant now);
}
