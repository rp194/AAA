package io.rp194.aaa.radius;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RadiusDictionary {
  public static final int VENDOR_SPECIFIC_TYPE = 26;

  public enum AttributeFormat {
    STRING,
    INTEGER,
    IP_ADDRESS,
    OCTETS
  }

  public record StandardAttribute(int type, String name, AttributeFormat format) {}
  public record VendorAttribute(int vendorId, int vendorType, String name, AttributeFormat format) {}

  private final Map<Integer, StandardAttribute> standardAttributes;
  private final Map<Integer, Map<Integer, VendorAttribute>> vendorAttributes;
  private final Map<String, StandardAttribute> standardByName;
  private final Map<String, VendorAttribute> vendorByName;

  public RadiusDictionary(Map<Integer, StandardAttribute> standardAttributes, Map<Integer, Map<Integer, VendorAttribute>> vendorAttributes) {
    this.standardAttributes = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(standardAttributes, "standardAttributes")));
    Map<Integer, Map<Integer, VendorAttribute>> vendor = new HashMap<>();
    for (Map.Entry<Integer, Map<Integer, VendorAttribute>> entry : vendorAttributes.entrySet()) {
      vendor.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
    }
    this.vendorAttributes = Collections.unmodifiableMap(vendor);
    Map<String, StandardAttribute> standardByName = new HashMap<>();
    for (StandardAttribute attr : this.standardAttributes.values()) {
      standardByName.put(attr.name(), attr);
    }
    this.standardByName = Collections.unmodifiableMap(standardByName);
    Map<String, VendorAttribute> vendorByName = new HashMap<>();
    for (Map<Integer, VendorAttribute> values : this.vendorAttributes.values()) {
      for (VendorAttribute attr : values.values()) {
        vendorByName.put(attr.name(), attr);
      }
    }
    this.vendorByName = Collections.unmodifiableMap(vendorByName);
  }

  public static RadiusDictionary defaultDictionary() {
    Map<Integer, StandardAttribute> standard = new HashMap<>();
    standard.put(1, new StandardAttribute(1, "User-Name", AttributeFormat.STRING));
    standard.put(2, new StandardAttribute(2, "User-Password", AttributeFormat.OCTETS));
    standard.put(4, new StandardAttribute(4, "NAS-IP-Address", AttributeFormat.IP_ADDRESS));
    standard.put(5, new StandardAttribute(5, "NAS-Port", AttributeFormat.INTEGER));
    standard.put(6, new StandardAttribute(6, "Service-Type", AttributeFormat.INTEGER));
    standard.put(7, new StandardAttribute(7, "Framed-Protocol", AttributeFormat.INTEGER));
    standard.put(8, new StandardAttribute(8, "Framed-IP-Address", AttributeFormat.IP_ADDRESS));
    standard.put(31, new StandardAttribute(31, "Calling-Station-Id", AttributeFormat.STRING));
    standard.put(32, new StandardAttribute(32, "NAS-Identifier", AttributeFormat.STRING));
    standard.put(40, new StandardAttribute(40, "Acct-Status-Type", AttributeFormat.INTEGER));
    standard.put(41, new StandardAttribute(41, "Acct-Delay-Time", AttributeFormat.INTEGER));
    standard.put(42, new StandardAttribute(42, "Acct-Input-Octets", AttributeFormat.INTEGER));
    standard.put(43, new StandardAttribute(43, "Acct-Output-Octets", AttributeFormat.INTEGER));
    standard.put(44, new StandardAttribute(44, "Acct-Session-Id", AttributeFormat.STRING));
    standard.put(45, new StandardAttribute(45, "Acct-Authentic", AttributeFormat.INTEGER));
    standard.put(46, new StandardAttribute(46, "Acct-Session-Time", AttributeFormat.INTEGER));
    standard.put(47, new StandardAttribute(47, "Acct-Input-Packets", AttributeFormat.INTEGER));
    standard.put(48, new StandardAttribute(48, "Acct-Output-Packets", AttributeFormat.INTEGER));
    standard.put(49, new StandardAttribute(49, "Acct-Terminate-Cause", AttributeFormat.INTEGER));
    standard.put(50, new StandardAttribute(50, "Acct-Multi-Session-Id", AttributeFormat.STRING));
    standard.put(55, new StandardAttribute(55, "Event-Timestamp", AttributeFormat.INTEGER));
    standard.put(61, new StandardAttribute(61, "NAS-Port-Type", AttributeFormat.INTEGER));
    standard.put(77, new StandardAttribute(77, "Connect-Info", AttributeFormat.STRING));
    standard.put(79, new StandardAttribute(79, "EAP-Message", AttributeFormat.OCTETS));
    standard.put(80, new StandardAttribute(80, "Message-Authenticator", AttributeFormat.OCTETS));
    standard.put(85, new StandardAttribute(85, "Acct-Interim-Interval", AttributeFormat.INTEGER));
    standard.put(87, new StandardAttribute(87, "NAS-Port-Id", AttributeFormat.STRING));
    standard.put(88, new StandardAttribute(88, "Framed-Pool", AttributeFormat.STRING));
    standard.put(250, new StandardAttribute(250, "Attr-250", AttributeFormat.STRING));

    Map<Integer, Map<Integer, VendorAttribute>> vsa = new HashMap<>();
    vsa.put(14988, Map.of(
        8, new VendorAttribute(14988, 8, "Mikrotik-Rate-Limit", AttributeFormat.STRING),
        19, new VendorAttribute(14988, 19, "Mikrotik-Address-List", AttributeFormat.STRING)));
    vsa.put(9, Map.of(1, new VendorAttribute(9, 1, "Cisco-AVPair", AttributeFormat.STRING)));
    return new RadiusDictionary(standard, vsa);
  }

  public Optional<String> standardName(int type) { return Optional.ofNullable(standardAttributes.get(type)).map(StandardAttribute::name); }

  public Optional<String> vendorName(int vendorId, int vendorType) {
    return Optional.ofNullable(vendorAttributes.getOrDefault(vendorId, Map.of()).get(vendorType)).map(VendorAttribute::name);
  }

  public Optional<StandardAttribute> standardDefinition(int type) {
    return Optional.ofNullable(standardAttributes.get(type));
  }

  public Optional<StandardAttribute> standardDefinition(String name) {
    return Optional.ofNullable(standardByName.get(name));
  }

  public Optional<VendorAttribute> vendorDefinition(int vendorId, int vendorType) {
    return Optional.ofNullable(vendorAttributes.getOrDefault(vendorId, Map.of()).get(vendorType));
  }

  public Optional<VendorAttribute> vendorDefinition(String name) {
    return Optional.ofNullable(vendorByName.get(name));
  }
}
