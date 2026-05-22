package io.rp194.aaa.radius;

import java.time.Duration;
import java.util.Set;

public final class RadiusValidationPolicy {
  private final boolean requireMessageAuthenticator;
  private final boolean includeMessageAuthenticatorInResponses;
  private final Duration replayWindow;
  private final Set<RadiusCode> supportedCodes;

  public RadiusValidationPolicy(boolean requireMessageAuthenticator,
                                boolean includeMessageAuthenticatorInResponses,
                                Duration replayWindow,
                                Set<RadiusCode> supportedCodes) {
    this.requireMessageAuthenticator = requireMessageAuthenticator;
    this.includeMessageAuthenticatorInResponses = includeMessageAuthenticatorInResponses;
    this.replayWindow = replayWindow;
    this.supportedCodes = Set.copyOf(supportedCodes);
  }

  public static RadiusValidationPolicy productionDefaults() {
    return new RadiusValidationPolicy(true, true, Duration.ofMinutes(5),
        Set.of(RadiusCode.ACCESS_REQUEST, RadiusCode.ACCOUNTING_REQUEST));
  }

  public static RadiusValidationPolicy testingDefaults() {
    return new RadiusValidationPolicy(false, true, Duration.ZERO,
        Set.of(RadiusCode.ACCESS_REQUEST, RadiusCode.ACCOUNTING_REQUEST));
  }

  public boolean requireMessageAuthenticator() {
    return requireMessageAuthenticator;
  }

  public boolean includeMessageAuthenticatorInResponses() {
    return includeMessageAuthenticatorInResponses;
  }

  public Duration replayWindow() {
    return replayWindow;
  }

  public Set<RadiusCode> supportedCodes() {
    return supportedCodes;
  }
}
