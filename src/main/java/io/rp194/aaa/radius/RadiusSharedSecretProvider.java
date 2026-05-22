package io.rp194.aaa.radius;

import java.net.InetSocketAddress;
import java.util.Optional;

public interface RadiusSharedSecretProvider {
  Optional<char[]> lookupSecret(String tenantId, String nasIp, String nasIdentifier, InetSocketAddress remoteAddress);
}
