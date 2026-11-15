package io.rp194.aaa.vendor;

import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.RadiusAttribute;
import java.util.ArrayList;
import java.util.List;

public final class MikroTikMapper implements VendorMapper {
  @Override
  public List<RadiusAttribute> mapAttributes(MapperContext context) {
    UserProfile profile = context.getProfile();
    List<RadiusAttribute> attributes = new ArrayList<>();
    if (profile.getRateLimit() != null && !profile.getRateLimit().isBlank()) {
      attributes.add(new RadiusAttribute("Mikrotik-Rate-Limit", profile.getRateLimit()));
    }
    if (profile.getAddressList() != null && !profile.getAddressList().isBlank()) {
      attributes.add(new RadiusAttribute("Mikrotik-Address-List", profile.getAddressList()));
    }
    return attributes;
  }
}
