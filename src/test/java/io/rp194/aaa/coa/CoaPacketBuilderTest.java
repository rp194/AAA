package io.rp194.aaa.coa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rp194.aaa.device.VendorType;
import io.rp194.aaa.radius.RadiusAttribute;
import io.rp194.aaa.radius.RadiusCode;
import io.rp194.aaa.radius.RadiusPacket;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoaPacketBuilderTest {
  @Test
  void buildsCoaPacketWithSessionIdentifiersAndVendorAttributes() {
    DefaultVendorEnforcementMapperRegistry registry = new DefaultVendorEnforcementMapperRegistry(
        new MikroTikEnforcementMapper(), new CiscoEnforcementMapper(), action -> List.of());
    CoaPacketBuilder builder = new CoaPacketBuilder(registry);

    CoaAction action = new CoaAction("tenant-a", "user-a", "sess-1", "198.51.100.20", "192.0.2.1",
        "55", "ge-0/0/1", "aa:bb:cc:dd:ee:ff", VendorType.MIKROTIK,
        CoaActionType.SUSPEND, "idem-1");
    RadiusPacket packet = builder.build(action, 10);

    assertEquals(RadiusCode.COA_REQUEST, packet.getCode());
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("User-Name", "user-a")));
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("Acct-Session-Id", "sess-1")));
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("Framed-IP-Address", "198.51.100.20")));
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("NAS-Port", "55")));
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("NAS-Port-Id", "ge-0/0/1")));
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("Calling-Station-Id", "aa:bb:cc:dd:ee:ff")));
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("Mikrotik-Address-List", "suspended")));
  }

  @Test
  void buildsCiscoCoaPacketWithRequiredIdentifiers() {
    DefaultVendorEnforcementMapperRegistry registry = new DefaultVendorEnforcementMapperRegistry(
        new MikroTikEnforcementMapper(), new CiscoEnforcementMapper(), action -> List.of());
    CoaPacketBuilder builder = new CoaPacketBuilder(registry);

    CoaAction action = new CoaAction("tenant-a", "user-a", "sess-2", "198.51.100.21", "192.0.2.2",
        "99", null, "11:22:33:44:55:66", VendorType.CISCO,
        CoaActionType.CAP_REACHED, "idem-2");
    RadiusPacket packet = builder.build(action, 12);

    assertTrue(packet.getAttributes().contains(new RadiusAttribute("NAS-Port", "99")));
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("Calling-Station-Id", "11:22:33:44:55:66")));
    assertTrue(packet.getAttributes().contains(new RadiusAttribute("Cisco-AVPair", "subscriber:command=activate-service level-capped")));
  }
}
