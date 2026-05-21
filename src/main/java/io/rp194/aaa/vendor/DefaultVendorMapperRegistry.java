package io.rp194.aaa.vendor;

import io.rp194.aaa.device.VendorType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultVendorMapperRegistry implements VendorMapperRegistry {
  private final Map<VendorType, VendorMapper> mappers = new EnumMap<>(VendorType.class);
  private final VendorMapper fallbackMapper;

  public DefaultVendorMapperRegistry(VendorMapper mikroTikMapper, VendorMapper ciscoMapper, VendorMapper fallbackMapper) {
    this.fallbackMapper = Objects.requireNonNull(fallbackMapper, "fallbackMapper");
    mappers.put(VendorType.MIKROTIK, Objects.requireNonNull(mikroTikMapper, "mikroTikMapper"));
    mappers.put(VendorType.CISCO, Objects.requireNonNull(ciscoMapper, "ciscoMapper"));
    mappers.put(VendorType.GENERIC, fallbackMapper);
  }

  @Override
  public VendorMapper mapperFor(VendorType vendorType) {
    return mappers.getOrDefault(vendorType, fallbackMapper);
  }
}
