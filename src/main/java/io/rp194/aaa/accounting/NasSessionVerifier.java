package io.rp194.aaa.accounting;

import io.rp194.aaa.session.SessionRecord;

public interface NasSessionVerifier {
  VerificationResult verify(SessionRecord record);

  final class VerificationResult {
    private final boolean alive;
    private final SessionClosureReason reason;

    public VerificationResult(boolean alive, SessionClosureReason reason) {
      this.alive = alive;
      this.reason = reason;
    }

    public static VerificationResult alive() {
      return new VerificationResult(true, null);
    }

    public static VerificationResult dead(SessionClosureReason reason) {
      return new VerificationResult(false, reason);
    }

    public boolean isAlive() {
      return alive;
    }

    public SessionClosureReason getReason() {
      return reason;
    }
  }
}
