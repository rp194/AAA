package io.rp194.aaa.vendor;

import io.rp194.aaa.device.VendorType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryTemplateRepository implements TemplateRepository {
  private final List<TemplateDefinition> templates = new ArrayList<>();

  public void save(TemplateDefinition definition) {
    templates.add(Objects.requireNonNull(definition, "definition"));
  }

  @Override
  public Optional<TemplateDefinition> findTemplate(String tenantId, VendorType vendorType, String deviceKey, String subjectKey, Instant now) {
    int bucket = Math.floorMod(Objects.requireNonNull(subjectKey, "subjectKey").hashCode(), 100);
    return templates.stream()
        .filter(t -> t.tenantId().equals(tenantId))
        .filter(t -> t.vendorType() == vendorType)
        .filter(t -> t.deviceKey() == null || t.deviceKey().equals(deviceKey))
        .filter(t -> !now.isBefore(t.effectiveFrom()))
        .filter(t -> bucket < t.rolloutPercent())
        .max((a, b) -> a.version().compareTo(b.version()));
  }
}
