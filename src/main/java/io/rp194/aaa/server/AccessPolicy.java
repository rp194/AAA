package io.rp194.aaa.server;

import java.util.Set;

public final class AccessPolicy {
  public enum ConcurrencyPolicy {
    REJECT,
    POD_OLDEST
  }

  private final int maxTenantSessions;
  private final boolean enforceMacBinding;
  private final Set<String> macBindingExceptionNasIds;
  private final int staleSessionGraceSeconds;
  private final ConcurrencyPolicy concurrencyPolicy;

  public AccessPolicy(int maxTenantSessions,
                      boolean enforceMacBinding,
                      Set<String> macBindingExceptionNasIds,
                      int staleSessionGraceSeconds,
                      ConcurrencyPolicy concurrencyPolicy) {
    this.maxTenantSessions = maxTenantSessions;
    this.enforceMacBinding = enforceMacBinding;
    this.macBindingExceptionNasIds = Set.copyOf(macBindingExceptionNasIds);
    this.staleSessionGraceSeconds = staleSessionGraceSeconds;
    this.concurrencyPolicy = concurrencyPolicy;
  }

  public static AccessPolicy defaults() {
    return new AccessPolicy(0, false, Set.of(), 0, ConcurrencyPolicy.REJECT);
  }

  public int getMaxTenantSessions() {
    return maxTenantSessions;
  }

  public boolean isEnforceMacBinding() {
    return enforceMacBinding;
  }

  public Set<String> getMacBindingExceptionNasIds() {
    return macBindingExceptionNasIds;
  }

  public int getStaleSessionGraceSeconds() {
    return staleSessionGraceSeconds;
  }

  public ConcurrencyPolicy getConcurrencyPolicy() {
    return concurrencyPolicy;
  }
}
