package io.rp194.aaa.radius;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RadiusDictionary {
  public static final int VENDOR_SPECIFIC_TYPE = 26;

  private final Map<Integer, String> standardAttributes;
  private final Map<Integer, Map<Integer, String>> vendorAttributes;

  public RadiusDictionary(Map<Integer, String> standardAttributes, Map<Integer, Map<Integer, String>> vendorAttributes) {
    this.standardAttributes = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(standardAttributes, "standardAttributes")));
    Map<Integer, Map<Integer, String>> vendor = new HashMap<>();
    for (Map.Entry<Integer, Map<Integer, String>> entry : vendorAttributes.entrySet()) {
      vendor.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
    }
    this.vendorAttributes = Collections.unmodifiableMap(vendor);
  }

  public static RadiusDictionary defaultDictionary() {
    Map<Integer, String> standard = new HashMap<>();
    standard.put(1, "User-Name");
    standard.put(4, "NAS-IP-Address");
    standard.put(5, "NAS-Port");
    standard.put(6, "Service-Type");
    standard.put(31, "Calling-Station-Id");
    standard.put(32, "NAS-Identifier");
    standard.put(40, "Acct-Status-Type");
    standard.put(44, "Acct-Session-Id");
    standard.put(8, "Framed-IP-Address");

    Map<Integer, Map<Integer, String>> vsa = new HashMap<>();
    vsa.put(14988, Map.of(8, "Mikrotik-Rate-Limit"));
    vsa.put(9, Map.of(1, "Cisco-AVPair"));
    return new RadiusDictionary(standard, vsa);
  }

  public Optional<String> standardName(int type) { return Optional.ofNullable(standardAttributes.get(type)); }

  public Optional<String> vendorName(int vendorId, int vendorType) {
    return Optional.ofNullable(vendorAttributes.getOrDefault(vendorId, Map.of()).get(vendorType));
  }
}
