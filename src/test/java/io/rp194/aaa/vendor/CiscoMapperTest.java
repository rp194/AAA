package io.rp194.aaa.vendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.AccessRequest;
import io.rp194.aaa.radius.RadiusAttribute;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CiscoMapperTest {
  @Test
  void mapsQosPolicy() {
    UserProfile profile = new UserProfile("tenant-a", "user-a", null, null, "gold", 0, 0);
    CiscoMapper mapper = new CiscoMapper();

    MapperContext context = new MapperContext(profile, AccessRequest.builder()
        .tenantId("tenant-a").username("user-a").sessionId("s1").nasIp("1.1.1.1").build(), null, Instant.EPOCH);
    List<RadiusAttribute> attributes = mapper.mapAttributes(context);

    assertEquals(1, attributes.size());
    assertTrue(attributes.contains(new RadiusAttribute("Cisco-AVPair", "qos-policy=gold")));
  }
}
