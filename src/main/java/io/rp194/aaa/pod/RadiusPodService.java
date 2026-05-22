package io.rp194.aaa.pod;

import io.rp194.aaa.radius.RadiusPacket;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class RadiusPodService implements PodService {
  private static final int POD_PORT = 3799;

  private final PodPacketBuilder packetBuilder;
  private final PodClient client;
  private final int maxRetries;

  public RadiusPodService(PodPacketBuilder packetBuilder, PodClient client, int maxRetries) {
    this.packetBuilder = Objects.requireNonNull(packetBuilder, "packetBuilder");
    this.client = Objects.requireNonNull(client, "client");
    this.maxRetries = maxRetries;
  }

  @Override
  public PodResult disconnect(PodAction action) {
    int attempt = 0;
    while (attempt <= maxRetries) {
      attempt++;
      RadiusPacket packet = packetBuilder.build(action, ThreadLocalRandom.current().nextInt(0, 256));
      PodResult result = client.send(action.getNasIp(), POD_PORT, packet);
      if (result == PodResult.ACK || result == PodResult.NACK) {
        return result;
      }
    }
    return PodResult.TIMEOUT;
  }
}
