package io.rp194.aaa.vendor;

import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.RadiusAttribute;
import java.util.List;

public interface VendorMapper {
  List<RadiusAttribute> mapAttributes(UserProfile profile);
}
