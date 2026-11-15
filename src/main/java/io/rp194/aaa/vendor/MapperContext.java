package io.rp194.aaa.vendor;

import io.rp194.aaa.device.DeviceProfile;
import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.radius.AccessRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MapperContext {
  private final UserProfile profile;
  private final AccessRequest request;
  private final DeviceProfile deviceProfile;
  private final Instant now;

  public MapperContext(UserProfile profile, AccessRequest request, DeviceProfile deviceProfile, Instant now) {
    this.profile = Objects.requireNonNull(profile, "profile");
    this.request = Objects.requireNonNull(request, "request");
    this.deviceProfile = deviceProfile;
    this.now = Objects.requireNonNull(now, "now");
  }

  public UserProfile getProfile() { return profile; }
  public AccessRequest getRequest() { return request; }
  public Optional<DeviceProfile> getDeviceProfile() { return Optional.ofNullable(deviceProfile); }
  public Instant getNow() { return now; }

  public Map<String, String> variables() {
    Map<String, String> vars = new HashMap<>();
    vars.put("tenantId", profile.getTenantId());
    vars.put("username", profile.getUsername());
    vars.put("profile.rateLimit", nullSafe(profile.getRateLimit()));
    vars.put("profile.addressList", nullSafe(profile.getAddressList()));
    vars.put("profile.qosPolicy", nullSafe(profile.getQosPolicy()));
    vars.put("session.sessionId", request.getSessionId());
    vars.put("session.nasIp", request.getNasIp());
    vars.put("session.macAddress", nullSafe(request.getMacAddress()));
    vars.put("package.name", request.findAttribute("Package-Name").orElse(""));
    vars.put("now.epochSecond", String.valueOf(now.getEpochSecond()));
    if (deviceProfile != null) {
      vars.put("device.nasIdentifier", nullSafe(deviceProfile.getNasIdentifier()));
      vars.put("device.displayName", nullSafe(deviceProfile.getDisplayName()));
      vars.put("device.vendor", deviceProfile.getVendorType().name());
    }
    return vars;
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
  }
}
