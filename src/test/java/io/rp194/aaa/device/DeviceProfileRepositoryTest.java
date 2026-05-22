package io.rp194.aaa.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeviceProfileRepositoryTest {
  @Test
  void findsByNasIdentifierOrIp() {
    InMemoryDeviceProfileRepository repository = new InMemoryDeviceProfileRepository();
    DeviceProfile profile = new DeviceProfile("tenant-a", "192.0.2.10", "nas-01", VendorType.MIKROTIK, "NAS 1");
    repository.register(profile);

    Optional<DeviceProfile> byIdentifier = repository.findByNas("tenant-a", "192.0.2.99", "nas-01");
    Optional<DeviceProfile> byIp = repository.findByNas("tenant-a", "192.0.2.10", null);

    assertTrue(byIdentifier.isPresent());
    assertEquals(VendorType.MIKROTIK, byIdentifier.get().getVendorType());
    assertTrue(byIp.isPresent());
    assertEquals("192.0.2.10", byIp.get().getNasIp());
  }

  @Test
  void prefersIdentifierMatchOverIpMatch() {
    InMemoryDeviceProfileRepository repository = new InMemoryDeviceProfileRepository();
    DeviceProfile byIdentifier = new DeviceProfile("tenant-a", "192.0.2.10", "nas-01", VendorType.MIKROTIK, "NAS 1");
    DeviceProfile byIp = new DeviceProfile("tenant-a", "192.0.2.99", "nas-99", VendorType.CISCO, "NAS 2");
    repository.register(byIdentifier);
    repository.register(byIp);

    Optional<DeviceProfile> resolved = repository.findByNas("tenant-a", "192.0.2.99", "nas-01");

    assertTrue(resolved.isPresent());
    assertEquals("192.0.2.10", resolved.get().getNasIp());
    assertEquals(VendorType.MIKROTIK, resolved.get().getVendorType());
  }
}
