package io.rp194.aaa.coa;

import java.time.Instant;
import java.util.Objects;

public final class CoaAttemptRecord {
  private final String idempotencyKey;
  private final String sessionId;
  private final String username;
  private final CoaActionType actionType;
  private final int attemptNumber;
  private final CoaResult result;
  private final Instant timestamp;

  public CoaAttemptRecord(String idempotencyKey, String sessionId, String username,
                          CoaActionType actionType, int attemptNumber, CoaResult result, Instant timestamp) {
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.username = Objects.requireNonNull(username, "username");
    this.actionType = Objects.requireNonNull(actionType, "actionType");
    this.attemptNumber = attemptNumber;
    this.result = Objects.requireNonNull(result, "result");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
  }

  public String getIdempotencyKey() { return idempotencyKey; }
  public String getSessionId() { return sessionId; }
  public String getUsername() { return username; }
  public CoaActionType getActionType() { return actionType; }
  public int getAttemptNumber() { return attemptNumber; }
  public CoaResult getResult() { return result; }
  public Instant getTimestamp() { return timestamp; }
}
