package io.rp194.aaa.device;

import java.util.Optional;

public interface DeviceProfileRepository {
  Optional<DeviceProfile> findByNas(String tenantId, String nasIp, String nasIdentifier);

  void register(DeviceProfile profile);
}
