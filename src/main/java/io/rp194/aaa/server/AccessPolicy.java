package io.rp194.aaa.server;

import java.util.Set;

public final class AccessPolicy {
  private final int maxTenantSessions;
  private final boolean enforceMacBinding;
  private final Set<String> macBindingExceptionNasIds;
  private final int staleSessionGraceSeconds;

  public AccessPolicy(int maxTenantSessions,
                      boolean enforceMacBinding,
                      Set<String> macBindingExceptionNasIds,
                      int staleSessionGraceSeconds) {
    this.maxTenantSessions = maxTenantSessions;
    this.enforceMacBinding = enforceMacBinding;
    this.macBindingExceptionNasIds = Set.copyOf(macBindingExceptionNasIds);
    this.staleSessionGraceSeconds = staleSessionGraceSeconds;
  }

  public static AccessPolicy defaults() {
    return new AccessPolicy(0, false, Set.of(), 0);
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
}
