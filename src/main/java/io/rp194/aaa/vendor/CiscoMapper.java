package io.rp194.aaa.vendor;

import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.RadiusAttribute;
import java.util.ArrayList;
import java.util.List;

public final class CiscoMapper implements VendorMapper {
  @Override
  public List<RadiusAttribute> mapAttributes(UserProfile profile) {
    List<RadiusAttribute> attributes = new ArrayList<>();
    if (profile.getQosPolicy() != null && !profile.getQosPolicy().isBlank()) {
      attributes.add(new RadiusAttribute("Cisco-AVPair", "qos-policy=" + profile.getQosPolicy()));
    }
    return attributes;
  }
}
