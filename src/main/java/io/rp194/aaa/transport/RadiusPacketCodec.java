package io.rp194.aaa.transport;

import io.rp194.aaa.accounting.InterimUpdate;
import io.rp194.aaa.radius.AccessRequest;
import io.rp194.aaa.radius.RadiusAttribute;
import io.rp194.aaa.radius.RadiusDictionary;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RadiusPacketCodec {
  private static final int HEADER_SIZE = 20;

  public enum DecodedType { ACCESS, ACCOUNTING }

  public record Decoded(DecodedType type, int identifier, AccessRequest accessRequest, InterimUpdate interimUpdate) {}

  private final RadiusDictionary dictionary;

  public RadiusPacketCodec() { this(RadiusDictionary.defaultDictionary()); }

  public RadiusPacketCodec(RadiusDictionary dictionary) {
    this.dictionary = Objects.requireNonNull(dictionary, "dictionary");
  }

  public Decoded decode(byte[] payload) {
    if (payload.length < HEADER_SIZE) throw new IllegalArgumentException("payload too short");
    ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
    int code = Byte.toUnsignedInt(buffer.get());
    int identifier = Byte.toUnsignedInt(buffer.get());
    int length = Short.toUnsignedInt(buffer.getShort());
    if (length < HEADER_SIZE || length > payload.length) throw new IllegalArgumentException("invalid radius packet length");
    if (code != 1 && code != 4) throw new IllegalArgumentException("unsupported radius code");
    buffer.position(HEADER_SIZE);

    Map<String, String> attrs = new HashMap<>();
    while (buffer.position() < length) {
      int type = Byte.toUnsignedInt(buffer.get());
      int attrLen = Byte.toUnsignedInt(buffer.get());
      if (attrLen < 2 || buffer.position() + attrLen - 2 > length) throw new IllegalArgumentException("invalid attr length");
      byte[] value = new byte[attrLen - 2];
      buffer.get(value);
      decodeAttribute(type, value, attrs);
    }

    String tenant = required(attrs, "tenant");
    String username = required(attrs, "User-Name");
    String sessionId = required(attrs, "Acct-Session-Id");
    String nasIp = required(attrs, "NAS-IP-Address");
    int interimInterval = parseInt(attrs.getOrDefault("Acct-Interim-Interval", "300"), "Acct-Interim-Interval");
    List<RadiusAttribute> attributes = attrs.entrySet().stream()
        .map(entry -> new RadiusAttribute(entry.getKey(), entry.getValue()))
        .toList();
    if (code == 4) {
      int delaySeconds = parseInt(attrs.getOrDefault("Acct-Delay-Time", "0"), "Acct-Delay-Time");
      Instant eventTime = Instant.now().minusSeconds(delaySeconds);
      InterimUpdate interim = new InterimUpdate(
          tenant, sessionId, username, nasIp, attrs.get("Calling-Station-Id"),
          eventTime, parseLong(attrs.getOrDefault("Acct-Input-Octets", "0"), "Acct-Input-Octets"),
          parseLong(attrs.getOrDefault("Acct-Output-Octets", "0"), "Acct-Output-Octets"),
          interimInterval);
      return new Decoded(DecodedType.ACCOUNTING, identifier, null, interim);
    }

    AccessRequest access = AccessRequest.builder()
        .tenantId(tenant).username(username).sessionId(sessionId)
        .nasIp(nasIp).nasIdentifier(attrs.get("NAS-Identifier")).macAddress(attrs.get("Calling-Station-Id"))
        .framedIpAddress(attrs.get("Framed-IP-Address")).nasPort(attrs.get("NAS-Port")).nasPortId(attrs.get("NAS-Port-Id"))
        .interimIntervalSeconds(interimInterval).attributes(attributes).build();
    return new Decoded(DecodedType.ACCESS, identifier, access, null);
  }

  private void decodeAttribute(int type, byte[] value, Map<String, String> out) {
    if (type == RadiusDictionary.VENDOR_SPECIFIC_TYPE && value.length >= 6) {
      ByteBuffer vsa = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN);
      int vendorId = vsa.getInt();
      int vendorType = Byte.toUnsignedInt(vsa.get());
      int vendorLength = Byte.toUnsignedInt(vsa.get());
      if (vendorLength >= 2 && vendorLength - 2 <= vsa.remaining()) {
        byte[] inner = new byte[vendorLength - 2];
        vsa.get(inner);
        String key = dictionary.vendorName(vendorId, vendorType).orElse("VSA-" + vendorId + "-" + vendorType);
        out.put(key, decodeValue(inner));
      }
      return;
    }
    if (type == 250) { out.put("tenant", decodeValue(value)); return; }
    String key = dictionary.standardName(type).orElse("Attr-" + type);
    out.put(key, decodeValue(value));
  }

  private static String decodeValue(byte[] value) {
    return new String(value, StandardCharsets.UTF_8).trim();
  }

  private static String required(Map<String, String> attrs, String key) {
    String value = attrs.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("missing required attributes");
    }
    return value;
  }

  private static int parseInt(String value, String field) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("invalid numeric attribute: " + field, ex);
    }
  }

  private static long parseLong(String value, String field) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("invalid numeric attribute: " + field, ex);
    }
  }
}
