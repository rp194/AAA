package io.rp194.aaa.policy;

import java.time.Instant;
import java.util.Objects;

public final class PackageModel {
  private final long quotaOctets;
  private final ResetCycle resetCycle;
  private final FupProfile fupProfile;
  private final Instant expiry;

  public PackageModel(long quotaOctets, ResetCycle resetCycle, FupProfile fupProfile, Instant expiry) {
    this.quotaOctets = quotaOctets;
    this.resetCycle = Objects.requireNonNull(resetCycle, "resetCycle");
    this.fupProfile = Objects.requireNonNull(fupProfile, "fupProfile");
    this.expiry = Objects.requireNonNull(expiry, "expiry");
  }

  public long getQuotaOctets() { return quotaOctets; }
  public ResetCycle getResetCycle() { return resetCycle; }
  public FupProfile getFupProfile() { return fupProfile; }
  public Instant getExpiry() { return expiry; }

  public enum ResetCycle { DAILY, WEEKLY, MONTHLY, NEVER }
  public enum FupProfile { NONE, THROTTLE, REDIRECT }
}
