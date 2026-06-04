package io.rp194.aaa.vendor;

import io.rp194.aaa.device.VendorType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultVendorMapperRegistry implements VendorMapperRegistry {
  private final Map<VendorType, VendorMapper> mappers = new EnumMap<>(VendorType.class);
  private final VendorMapper fallbackMapper;

  public DefaultVendorMapperRegistry(VendorMapper mikroTikMapper, VendorMapper ciscoMapper, VendorMapper fallbackMapper) {
    this(mikroTikMapper, ciscoMapper, fallbackMapper, null);
  }

  public DefaultVendorMapperRegistry(VendorMapper mikroTikMapper,
                                     VendorMapper ciscoMapper,
                                     VendorMapper fallbackMapper,
                                     TemplateRepository templateRepository) {
    this.fallbackMapper = Objects.requireNonNull(fallbackMapper, "fallbackMapper");
    VendorMapper mikrotikBase = Objects.requireNonNull(mikroTikMapper, "mikroTikMapper");
    VendorMapper ciscoBase = Objects.requireNonNull(ciscoMapper, "ciscoMapper");
    if (templateRepository != null) {
      mikrotikBase = new TemplateVendorMapper(VendorType.MIKROTIK, templateRepository, mikrotikBase);
      ciscoBase = new TemplateVendorMapper(VendorType.CISCO, templateRepository, ciscoBase);
    }
    mappers.put(VendorType.MIKROTIK, mikrotikBase);
    mappers.put(VendorType.CISCO, ciscoBase);
    mappers.put(VendorType.GENERIC, fallbackMapper);
  }

  @Override
  public VendorMapper mapperFor(VendorType vendorType) {
    return mappers.getOrDefault(vendorType, fallbackMapper);
  }
}
