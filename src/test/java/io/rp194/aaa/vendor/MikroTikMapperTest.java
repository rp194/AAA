package io.rp194.aaa.vendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.AccessRequest;
import io.rp194.aaa.radius.RadiusAttribute;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MikroTikMapperTest {
  @Test
  void mapsRateLimitAndAddressList() {
    UserProfile profile = new UserProfile("tenant-a", "user-a", "10M/10M", "premium", null, 0, 0);
    MikroTikMapper mapper = new MikroTikMapper();

    MapperContext context = new MapperContext(profile, AccessRequest.builder()
        .tenantId("tenant-a").username("user-a").sessionId("s1").nasIp("1.1.1.1").build(), null, Instant.EPOCH);
    List<RadiusAttribute> attributes = mapper.mapAttributes(context);

    assertEquals(2, attributes.size());
    assertTrue(attributes.contains(new RadiusAttribute("Mikrotik-Rate-Limit", "10M/10M")));
    assertTrue(attributes.contains(new RadiusAttribute("Mikrotik-Address-List", "premium")));
  }
}
