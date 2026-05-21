package io.rp194.aaa.coa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryCoaAuditStore implements CoaAuditStore {
  private final CopyOnWriteArrayList<CoaAttemptRecord> records = new CopyOnWriteArrayList<>();

  @Override
  public void saveAttempt(CoaAttemptRecord record) {
    records.add(record);
  }

  @Override
  public List<CoaAttemptRecord> allAttempts() {
    return new ArrayList<>(records);
  }

  @Override
  public Optional<CoaAttemptRecord> latestForIdempotencyKey(String idempotencyKey) {
    return records.stream()
        .filter(record -> record.getIdempotencyKey().equals(idempotencyKey))
        .max(Comparator.comparing(CoaAttemptRecord::getAttemptNumber));
  }
}
