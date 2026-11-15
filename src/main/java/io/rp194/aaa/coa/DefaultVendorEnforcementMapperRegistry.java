package io.rp194.aaa.coa;

import io.rp194.aaa.device.VendorType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultVendorEnforcementMapperRegistry {
  private final Map<VendorType, VendorEnforcementMapper> mappers = new EnumMap<>(VendorType.class);

  public DefaultVendorEnforcementMapperRegistry(VendorEnforcementMapper mikrotik, VendorEnforcementMapper cisco,
                                                 VendorEnforcementMapper fallback) {
    mappers.put(VendorType.MIKROTIK, Objects.requireNonNull(mikrotik, "mikrotik"));
    mappers.put(VendorType.CISCO, Objects.requireNonNull(cisco, "cisco"));
    mappers.put(VendorType.GENERIC, Objects.requireNonNull(fallback, "fallback"));
  }

  public VendorEnforcementMapper mapperFor(VendorType vendorType) {
    return mappers.getOrDefault(vendorType, mappers.get(VendorType.GENERIC));
  }
}
