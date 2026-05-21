package io.rp194.aaa.transport;

import io.rp194.aaa.accounting.InterimUpdate;
import io.rp194.aaa.radius.AccessRequest;
import io.rp194.aaa.radius.RadiusDictionary;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
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

    if (code == 4) {
      InterimUpdate interim = new InterimUpdate(
          attrs.get("tenant"), attrs.get("Acct-Session-Id"), attrs.get("User-Name"), attrs.get("NAS-IP-Address"), attrs.get("Calling-Station-Id"),
          Instant.now(), Long.parseLong(attrs.getOrDefault("Acct-Input-Octets", "0")), Long.parseLong(attrs.getOrDefault("Acct-Output-Octets", "0")),
          Integer.parseInt(attrs.getOrDefault("Acct-Interim-Interval", "300")));
      return new Decoded(DecodedType.ACCOUNTING, identifier, null, interim);
    }

    AccessRequest access = AccessRequest.builder()
        .tenantId(attrs.get("tenant")).username(attrs.get("User-Name")).sessionId(attrs.get("Acct-Session-Id"))
        .nasIp(attrs.getOrDefault("NAS-IP-Address", "0.0.0.0")).nasIdentifier(attrs.get("NAS-Identifier")).macAddress(attrs.get("Calling-Station-Id"))
        .interimIntervalSeconds(Integer.parseInt(attrs.getOrDefault("Acct-Interim-Interval", "300"))).build();
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
}
