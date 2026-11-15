package io.rp194.aaa.coa;

import io.rp194.aaa.radius.RadiusPacket;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class CoaService {
  private static final int COA_PORT = 3799;

  private final CoaPacketBuilder packetBuilder;
  private final CoaClient coaClient;
  private final CoaAuditStore auditStore;
  private final Clock clock;
  private final int maxRetries;

  public CoaService(CoaPacketBuilder packetBuilder, CoaClient coaClient, CoaAuditStore auditStore, Clock clock, int maxRetries) {
    this.packetBuilder = Objects.requireNonNull(packetBuilder, "packetBuilder");
    this.coaClient = Objects.requireNonNull(coaClient, "coaClient");
    this.auditStore = Objects.requireNonNull(auditStore, "auditStore");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.maxRetries = maxRetries;
  }

  public CoaResult applyPolicyAction(CoaAction action) {
    Optional<CoaAttemptRecord> latest = auditStore.latestForIdempotencyKey(action.getIdempotencyKey());
    if (latest.isPresent() && latest.get().getResult() == CoaResult.ACK) {
      return CoaResult.DUPLICATE;
    }

    int attempt = 0;
    while (attempt <= maxRetries) {
      attempt++;
      RadiusPacket packet = packetBuilder.build(action, ThreadLocalRandom.current().nextInt(0, 256));
      CoaResult result = coaClient.send(action.getNasIp(), COA_PORT, packet);
      auditStore.saveAttempt(new CoaAttemptRecord(
          action.getIdempotencyKey(), action.getSessionId(), action.getUsername(),
          action.getActionType(), attempt, result, clock.instant()));
      if (result == CoaResult.ACK) {
        return CoaResult.ACK;
      }
      if (result == CoaResult.NACK) {
        return CoaResult.NACK;
      }
    }
    return CoaResult.TIMEOUT;
  }
}
