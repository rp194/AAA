package io.rp194.aaa.session;

public final class SessionTtlPolicy {
  private final int multiplier;
  private final int graceSeconds;

  public SessionTtlPolicy(int multiplier, int graceSeconds) {
    if (multiplier < 1) {
      throw new IllegalArgumentException("multiplier must be >= 1");
    }
    if (graceSeconds < 0) {
      throw new IllegalArgumentException("graceSeconds must be >= 0");
    }
    this.multiplier = multiplier;
    this.graceSeconds = graceSeconds;
  }

  public static SessionTtlPolicy defaults() {
    return new SessionTtlPolicy(2, 30);
  }

  public int getMultiplier() {
    return multiplier;
  }

  public int getGraceSeconds() {
    return graceSeconds;
  }

  public int ttlSeconds(int interimIntervalSeconds) {
    long base = Math.max(interimIntervalSeconds, 1);
    long ttl = (base * multiplier) + graceSeconds;
    if (ttl > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return Math.max(1, (int) ttl);
  }
}
