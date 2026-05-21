package io.rp194.aaa.server;

import io.rp194.aaa.device.DeviceProfile;
import io.rp194.aaa.device.DeviceProfileRepository;
import io.rp194.aaa.device.VendorType;
import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.profile.UserProfileStore;
import io.rp194.aaa.radius.AccessRequest;
import io.rp194.aaa.radius.RadiusAttribute;
import io.rp194.aaa.radius.RadiusCode;
import io.rp194.aaa.radius.RadiusPacket;
import io.rp194.aaa.session.SessionRecord;
import io.rp194.aaa.session.SessionStore;
import io.rp194.aaa.vendor.VendorMapper;
import io.rp194.aaa.vendor.VendorMapperRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RadiusAccessHandler {
  private final DeviceProfileRepository deviceProfileRepository;
  private final UserProfileStore userProfileStore;
  private final VendorMapperRegistry vendorMapperRegistry;
  private final SessionStore sessionStore;
  private final Clock clock;
  private final AccessPolicy accessPolicy;
  private final AccessAuditLogger auditLogger;

  public RadiusAccessHandler(DeviceProfileRepository deviceProfileRepository,
                             UserProfileStore userProfileStore,
                             VendorMapperRegistry vendorMapperRegistry,
                             SessionStore sessionStore,
                             Clock clock) {
    this(deviceProfileRepository, userProfileStore, vendorMapperRegistry, sessionStore, clock,
        AccessPolicy.defaults(), AccessAuditLogger.NOOP);
  }

  public RadiusAccessHandler(DeviceProfileRepository deviceProfileRepository,
                             UserProfileStore userProfileStore,
                             VendorMapperRegistry vendorMapperRegistry,
                             SessionStore sessionStore,
                             Clock clock,
                             AccessPolicy accessPolicy,
                             AccessAuditLogger auditLogger) {
    this.deviceProfileRepository = Objects.requireNonNull(deviceProfileRepository, "deviceProfileRepository");
    this.userProfileStore = Objects.requireNonNull(userProfileStore, "userProfileStore");
    this.vendorMapperRegistry = Objects.requireNonNull(vendorMapperRegistry, "vendorMapperRegistry");
    this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
    this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
  }

  public RadiusPacket handleAccessRequest(AccessRequest request, int identifier) {
    Optional<UserProfile> profile = userProfileStore.findProfile(request.getTenantId(), request.getUsername());
    if (profile.isEmpty()) {
      auditLogger.log(new AccessAuditEvent("ACCESS_REJECT_UNKNOWN_USER", request.getTenantId(), request.getUsername(), Map.of()));
      return new RadiusPacket(RadiusCode.ACCESS_REJECT, identifier,
          List.of(new RadiusAttribute("Reply-Message", "Unknown subscriber")));
    }
    Instant now = clock.instant();

    List<SessionRecord> userSessions = sessionStore.findByTenantAndUsername(request.getTenantId(), request.getUsername());
    List<SessionRecord> tenantSessions = sessionStore.findByTenant(request.getTenantId());
    pruneStale(userSessions, now);
    pruneStale(tenantSessions, now);

    Optional<RadiusPacket> reject = enforcePolicies(request, profile.get(), userSessions, tenantSessions, now, identifier);
    if (reject.isPresent()) {
      return reject.get();
    }

    VendorType vendorType = deviceProfileRepository
        .findByNas(request.getTenantId(), request.getNasIp(), request.getNasIdentifier())
        .map(DeviceProfile::getVendorType)
        .orElse(VendorType.GENERIC);

    VendorMapper mapper = vendorMapperRegistry.mapperFor(vendorType);
    List<RadiusAttribute> responseAttributes = new ArrayList<>(mapper.mapAttributes(profile.get()));

    sessionStore.upsert(new SessionRecord(
        request.getTenantId(),
        request.getSessionId(),
        request.getUsername(),
        request.getNasIp(),
        request.getMacAddress(),
        now,
        now,
        0L,
        0L,
        request.getInterimIntervalSeconds()));

    auditLogger.log(new AccessAuditEvent("ACCESS_ACCEPT", request.getTenantId(), request.getUsername(),
        Map.of("sessionId", request.getSessionId())));

    return new RadiusPacket(RadiusCode.ACCESS_ACCEPT, identifier, responseAttributes);
  }

  private Optional<RadiusPacket> enforcePolicies(AccessRequest request,
                                                 UserProfile profile,
                                                 List<SessionRecord> userSessions,
                                                 List<SessionRecord> tenantSessions,
                                                 Instant now,
                                                 int identifier) {
    if (accessPolicy.isEnforceMacBinding()
        && !accessPolicy.getMacBindingExceptionNasIds().contains(request.getNasIdentifier())) {
      for (SessionRecord session : userSessions) {
        if (session.getMacAddress() != null && request.getMacAddress() != null
            && !session.getMacAddress().equals(request.getMacAddress())
            && !session.getSessionId().equals(request.getSessionId())) {
          return Optional.of(reject(identifier, request, "MAC binding conflict",
              "ACCESS_REJECT_MAC_BIND", Map.of("activeMac", session.getMacAddress(), "requestMac", request.getMacAddress())));
        }
      }
    }

    if (profile.getMaxConcurrentSessions() > 0 && userSessions.size() >= profile.getMaxConcurrentSessions()) {
      return Optional.of(reject(identifier, request, "Max concurrent sessions reached for user",
          "ACCESS_REJECT_USER_CONCURRENCY", Map.of("active", String.valueOf(userSessions.size()))));
    }
    if (accessPolicy.getMaxTenantSessions() > 0 && tenantSessions.size() >= accessPolicy.getMaxTenantSessions()) {
      return Optional.of(reject(identifier, request, "Tenant session limit reached",
          "ACCESS_REJECT_TENANT_CONCURRENCY", Map.of("active", String.valueOf(tenantSessions.size()))));
    }
    return Optional.empty();
  }

  private void pruneStale(List<SessionRecord> sessions, Instant now) {
    for (SessionRecord session : sessions) {
      if (session.expiresAt().plusSeconds(accessPolicy.getStaleSessionGraceSeconds()).isBefore(now)) {
        sessionStore.remove(session.getTenantId(), session.getSessionId());
      }
    }
  }

  private RadiusPacket reject(int identifier,
                              AccessRequest request,
                              String replyMessage,
                              String auditType,
                              Map<String, String> extraDetails) {
    Map<String, String> details = new HashMap<>(extraDetails);
    details.put("sessionId", request.getSessionId());
    details.put("nasIp", request.getNasIp());
    auditLogger.log(new AccessAuditEvent(auditType, request.getTenantId(), request.getUsername(), details));
    return new RadiusPacket(RadiusCode.ACCESS_REJECT, identifier,
        List.of(new RadiusAttribute("Reply-Message", replyMessage)));
  }
}
