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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RadiusAccessHandler {
  private final DeviceProfileRepository deviceProfileRepository;
  private final UserProfileStore userProfileStore;
  private final VendorMapperRegistry vendorMapperRegistry;
  private final SessionStore sessionStore;
  private final Clock clock;

  public RadiusAccessHandler(DeviceProfileRepository deviceProfileRepository,
                             UserProfileStore userProfileStore,
                             VendorMapperRegistry vendorMapperRegistry,
                             SessionStore sessionStore,
                             Clock clock) {
    this.deviceProfileRepository = Objects.requireNonNull(deviceProfileRepository, "deviceProfileRepository");
    this.userProfileStore = Objects.requireNonNull(userProfileStore, "userProfileStore");
    this.vendorMapperRegistry = Objects.requireNonNull(vendorMapperRegistry, "vendorMapperRegistry");
    this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public RadiusPacket handleAccessRequest(AccessRequest request, int identifier) {
    Optional<UserProfile> profile = userProfileStore.findProfile(request.getTenantId(), request.getUsername());
    if (profile.isEmpty()) {
      return new RadiusPacket(RadiusCode.ACCESS_REJECT, identifier,
          List.of(new RadiusAttribute("Reply-Message", "Unknown subscriber")));
    }

    VendorType vendorType = deviceProfileRepository
        .findByNas(request.getTenantId(), request.getNasIp(), request.getNasIdentifier())
        .map(DeviceProfile::getVendorType)
        .orElse(VendorType.GENERIC);

    VendorMapper mapper = vendorMapperRegistry.mapperFor(vendorType);
    List<RadiusAttribute> responseAttributes = new ArrayList<>(mapper.mapAttributes(profile.get()));

    Instant now = clock.instant();
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

    return new RadiusPacket(RadiusCode.ACCESS_ACCEPT, identifier, responseAttributes);
  }
}
