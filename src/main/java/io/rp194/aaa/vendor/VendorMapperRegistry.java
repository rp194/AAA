package io.rp194.aaa.vendor;

import io.rp194.aaa.device.VendorType;

public interface VendorMapperRegistry {
  VendorMapper mapperFor(VendorType vendorType);
}
