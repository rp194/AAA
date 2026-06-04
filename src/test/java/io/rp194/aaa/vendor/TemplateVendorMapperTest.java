package io.rp194.aaa.vendor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.rp194.aaa.device.DeviceProfile;
import io.rp194.aaa.device.VendorType;
import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.AccessRequest;
import io.rp194.aaa.radius.RadiusAttribute;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateVendorMapperTest {
  @Test
  void usesTemplateAndSubstitutesVariables() {
    InMemoryTemplateRepository repository = new InMemoryTemplateRepository();
    repository.save(new TemplateDefinition("tenant-a", VendorType.MIKROTIK, "10.0.0.1:nas-1", "v1", 100, Instant.EPOCH,
        List.of(new TemplateAttribute("Reply-Message", "hello ${username} ${package.name}"))));

    TemplateVendorMapper mapper = new TemplateVendorMapper(VendorType.MIKROTIK, repository, new GenericVendorMapper());
    MapperContext context = new MapperContext(
        new UserProfile("tenant-a", "user-a", null, null, null, 0, 0, 1000, 1000, "default"),
        AccessRequest.builder().tenantId("tenant-a").username("user-a").sessionId("s1").nasIp("10.0.0.1").nasIdentifier("nas-1")
            .addAttribute(new RadiusAttribute("Package-Name", "premium")).build(),
        new DeviceProfile("tenant-a", "10.0.0.1", "nas-1", VendorType.MIKROTIK, "NAS"),
        Instant.now());

    List<RadiusAttribute> output = mapper.mapAttributes(context);
    assertEquals(List.of(new RadiusAttribute("Reply-Message", "hello user-a premium")), output);
  }

  @Test
  void fallsBackWhenTemplateUnavailable() {
    TemplateVendorMapper mapper = new TemplateVendorMapper(VendorType.CISCO, new InMemoryTemplateRepository(), new CiscoMapper());
    MapperContext context = new MapperContext(
        new UserProfile("tenant-a", "user-a", null, null, "gold", 0, 0, 1000, 1000, "default"),
        AccessRequest.builder().tenantId("tenant-a").username("user-a").sessionId("s1").nasIp("1.1.1.1").build(),
        null,
        Instant.now());

    List<RadiusAttribute> output = mapper.mapAttributes(context);
    assertEquals(List.of(new RadiusAttribute("Cisco-AVPair", "qos-policy=gold")), output);
  }
}
