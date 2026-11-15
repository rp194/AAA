package io.rp194.aaa.coa;

import java.util.List;
import java.util.Optional;

public interface CoaAuditStore {
  void saveAttempt(CoaAttemptRecord record);
  List<CoaAttemptRecord> allAttempts();
  Optional<CoaAttemptRecord> latestForIdempotencyKey(String idempotencyKey);
}
