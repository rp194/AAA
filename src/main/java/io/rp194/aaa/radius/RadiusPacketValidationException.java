package io.rp194.aaa.radius;

public final class RadiusPacketValidationException extends RuntimeException {
  public enum Reason {
    INVALID_LENGTH,
    INVALID_AUTHENTICATOR,
    MISSING_REQUIRED_ATTRIBUTE,
    MISSING_SHARED_SECRET,
    UNSUPPORTED_CODE,
    INVALID_MESSAGE_AUTHENTICATOR,
    REPLAY_DETECTED
  }

  private final Reason reason;

  public RadiusPacketValidationException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }
}
