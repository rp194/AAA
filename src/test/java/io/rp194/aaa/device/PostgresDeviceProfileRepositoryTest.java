package io.rp194.aaa.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PostgresDeviceProfileRepositoryTest {
  @Test
  void prefersIdentifierLookupBeforeIpLookup() {
    FakeLookup lookup = new FakeLookup();
    DeviceProfile byId = new DeviceProfile("tenant-a", "192.0.2.10", "nas-01", VendorType.MIKROTIK, "NAS 1");
    DeviceProfile byIp = new DeviceProfile("tenant-a", "192.0.2.99", "nas-99", VendorType.CISCO, "NAS 2");
    lookup.identifierResult = Optional.of(byId);
    lookup.ipResult = Optional.of(byIp);

    PostgresDeviceProfileRepository repository = new PostgresDeviceProfileRepository(lookup);
    Optional<DeviceProfile> resolved = repository.findByNas("tenant-a", "192.0.2.99", "nas-01");

    assertTrue(resolved.isPresent());
    assertEquals("identifier", lookup.calls.get(0));
    assertEquals(1, lookup.calls.size());
    assertEquals(VendorType.MIKROTIK, resolved.get().getVendorType());
  }

  @Test
  void fallsBackToIpWhenIdentifierMissing() {
    FakeLookup lookup = new FakeLookup();
    DeviceProfile byIp = new DeviceProfile("tenant-a", "192.0.2.99", "nas-99", VendorType.CISCO, "NAS 2");
    lookup.identifierResult = Optional.empty();
    lookup.ipResult = Optional.of(byIp);

    PostgresDeviceProfileRepository repository = new PostgresDeviceProfileRepository(lookup);
    Optional<DeviceProfile> resolved = repository.findByNas("tenant-a", "192.0.2.99", "nas-01");

    assertTrue(resolved.isPresent());
    assertEquals(List.of("identifier", "ip"), lookup.calls);
    assertEquals("192.0.2.99", resolved.get().getNasIp());
  }

  private static final class FakeLookup implements PostgresDeviceProfileRepository.DeviceProfileLookup {
    private Optional<DeviceProfile> identifierResult = Optional.empty();
    private Optional<DeviceProfile> ipResult = Optional.empty();
    private final List<String> calls = new ArrayList<>();

    @Override
    public Optional<DeviceProfile> findByNasIdentifier(String tenantId, String nasIdentifier) {
      calls.add("identifier");
      return identifierResult;
    }

    @Override
    public Optional<DeviceProfile> findByNasIp(String tenantId, String nasIp) {
      calls.add("ip");
      return ipResult;
    }
  }
}
