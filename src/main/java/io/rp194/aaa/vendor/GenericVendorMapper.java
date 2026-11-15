package io.rp194.aaa.vendor;

import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.RadiusAttribute;
import java.util.Collections;
import java.util.List;

public final class GenericVendorMapper implements VendorMapper {
  @Override
  public List<RadiusAttribute> mapAttributes(MapperContext context) {
    return Collections.emptyList();
  }
}
