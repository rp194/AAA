package io.rp194.aaa.device;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryDeviceProfileRepository implements DeviceProfileRepository {
  private final Map<String, DeviceProfile> byIp = new ConcurrentHashMap<>();
  private final Map<String, DeviceProfile> byIdentifier = new ConcurrentHashMap<>();

  @Override
  public Optional<DeviceProfile> findByNas(String tenantId, String nasIp, String nasIdentifier) {
    Objects.requireNonNull(tenantId, "tenantId");
    if (nasIdentifier != null && !nasIdentifier.isBlank()) {
      DeviceProfile profile = byIdentifier.get(key(tenantId, nasIdentifier));
      if (profile != null) {
        return Optional.of(profile);
      }
    }
    return Optional.ofNullable(byIp.get(key(tenantId, nasIp)));
  }

  @Override
  public void register(DeviceProfile profile) {
    Objects.requireNonNull(profile, "profile");
    byIp.put(key(profile.getTenantId(), profile.getNasIp()), profile);
    if (profile.getNasIdentifier() != null && !profile.getNasIdentifier().isBlank()) {
      byIdentifier.put(key(profile.getTenantId(), profile.getNasIdentifier()), profile);
    }
  }

  private static String key(String tenantId, String value) {
    return tenantId + "::" + value;
  }
}
