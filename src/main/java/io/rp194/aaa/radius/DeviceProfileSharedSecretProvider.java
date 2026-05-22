package io.rp194.aaa.radius;

import io.rp194.aaa.device.DeviceProfile;
import io.rp194.aaa.device.DeviceProfileRepository;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;

public final class DeviceProfileSharedSecretProvider implements RadiusSharedSecretProvider {
  private final DeviceProfileRepository deviceProfileRepository;

  public DeviceProfileSharedSecretProvider(DeviceProfileRepository deviceProfileRepository) {
    this.deviceProfileRepository = Objects.requireNonNull(deviceProfileRepository, "deviceProfileRepository");
  }

  @Override
  public Optional<char[]> lookupSecret(String tenantId, String nasIp, String nasIdentifier, InetSocketAddress remoteAddress) {
    if (tenantId == null || tenantId.isBlank()) {
      return Optional.empty();
    }
    Optional<DeviceProfile> profile = deviceProfileRepository.findByNas(tenantId, nasIp, nasIdentifier);
    if (profile.isEmpty()) {
      return Optional.empty();
    }
    String secret = profile.get().getSharedSecret();
    if (secret == null || secret.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(secret.toCharArray());
  }
}
