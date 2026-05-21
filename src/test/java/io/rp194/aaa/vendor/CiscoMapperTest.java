package io.rp194.aaa.vendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.RadiusAttribute;
import java.util.List;
import org.junit.jupiter.api.Test;

class CiscoMapperTest {
  @Test
  void mapsQosPolicy() {
    UserProfile profile = new UserProfile("tenant-a", "user-a", null, null, "gold", 0, 0);
    CiscoMapper mapper = new CiscoMapper();

    List<RadiusAttribute> attributes = mapper.mapAttributes(profile);

    assertEquals(1, attributes.size());
    assertTrue(attributes.contains(new RadiusAttribute("Cisco-AVPair", "qos-policy=gold")));
  }
}
