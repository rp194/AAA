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
}
