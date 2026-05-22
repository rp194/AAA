package io.rp194.aaa.transport;

import io.rp194.aaa.accounting.InterimUpdate;
import io.rp194.aaa.radius.AccessRequest;
import io.rp194.aaa.radius.RadiusAttribute;
import io.rp194.aaa.radius.RadiusCode;
import io.rp194.aaa.radius.RadiusCrypto;
import io.rp194.aaa.radius.RadiusDictionary;
import io.rp194.aaa.radius.RadiusPacket;
import io.rp194.aaa.radius.RadiusPacketValidationException;
import io.rp194.aaa.radius.RadiusReplayCache;
import io.rp194.aaa.radius.RadiusSharedSecretProvider;
import io.rp194.aaa.radius.RadiusValidationPolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RadiusPacketCodec {
  private static final int HEADER_SIZE = 20;
  private static final int MESSAGE_AUTHENTICATOR_TYPE = 80;
  private static final int USER_PASSWORD_TYPE = 2;

  public enum DecodedType { ACCESS, ACCOUNTING }

  public record RequestContext(RadiusCode code,
                               int identifier,
                               byte[] requestAuthenticator,
                               byte[] sharedSecret,
                               boolean messageAuthenticatorPresent,
                               InetSocketAddress remoteAddress) {
    public void destroy() {
      RadiusCrypto.clear(sharedSecret);
      RadiusCrypto.clear(requestAuthenticator);
    }
  }

  public record Decoded(DecodedType type,
                        int identifier,
                        AccessRequest accessRequest,
                        InterimUpdate interimUpdate,
                        RequestContext context) {}

  private record RawAttribute(int type, Integer vendorId, Integer vendorType, byte[] value) {}

  private final RadiusDictionary dictionary;
  private final RadiusSharedSecretProvider sharedSecretProvider;
  private final RadiusReplayCache replayCache;
  private final Clock clock;
  private final RadiusValidationPolicy validationPolicy;

  public RadiusPacketCodec(RadiusDictionary dictionary,
                           RadiusSharedSecretProvider sharedSecretProvider,
                           RadiusReplayCache replayCache,
                           Clock clock,
                           RadiusValidationPolicy validationPolicy) {
    this.dictionary = Objects.requireNonNull(dictionary, "dictionary");
    this.sharedSecretProvider = Objects.requireNonNull(sharedSecretProvider, "sharedSecretProvider");
    this.replayCache = Objects.requireNonNull(replayCache, "replayCache");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.validationPolicy = Objects.requireNonNull(validationPolicy, "validationPolicy");
  }

  public Decoded decode(byte[] payload, InetSocketAddress remoteAddress) {
    Objects.requireNonNull(remoteAddress, "remoteAddress");
    if (payload.length < HEADER_SIZE) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_LENGTH, "payload too short");
    }
    ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
    int rawCode = Byte.toUnsignedInt(buffer.get());
    int identifier = Byte.toUnsignedInt(buffer.get());
    int length = Short.toUnsignedInt(buffer.getShort());
    if (length < HEADER_SIZE || length > payload.length) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_LENGTH, "invalid radius packet length");
    }

    RadiusCode code;
    try {
      code = RadiusCode.fromCode(rawCode);
    } catch (IllegalArgumentException ex) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.UNSUPPORTED_CODE, ex.getMessage());
    }
    if (!validationPolicy.supportedCodes().contains(code)) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.UNSUPPORTED_CODE, "Unsupported code: " + code);
    }

    byte[] requestAuthenticator = new byte[16];
    buffer.get(requestAuthenticator);
    buffer.position(HEADER_SIZE);

    Map<String, String> attrs = new HashMap<>();
    List<RadiusAttribute> attributes = new ArrayList<>();
    List<RawAttribute> rawAttributes = new ArrayList<>();
    MessageAuthenticator messageAuthenticator = null;
    byte[] userPassword = null;

    while (buffer.position() < length) {
      int type = Byte.toUnsignedInt(buffer.get());
      int attrLen = Byte.toUnsignedInt(buffer.get());
      if (attrLen < 2 || buffer.position() + attrLen - 2 > length) {
        throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_LENGTH, "invalid attr length");
      }
      byte[] value = new byte[attrLen - 2];
      int valueOffset = buffer.position();
      buffer.get(value);
      if (type == RadiusDictionary.VENDOR_SPECIFIC_TYPE) {
        RawAttribute vendorAttr = decodeVendor(value);
        if (vendorAttr != null) {
          rawAttributes.add(vendorAttr);
          String name = dictionary.vendorName(vendorAttr.vendorId(), vendorAttr.vendorType())
              .orElse("VSA-" + vendorAttr.vendorId() + "-" + vendorAttr.vendorType());
          String decoded = decodeValue(vendorAttr.value(), dictionary.vendorDefinition(name).map(RadiusDictionary.VendorAttribute::format).orElse(RadiusDictionary.AttributeFormat.STRING));
          attrs.put(name, decoded);
          attributes.add(new RadiusAttribute(name, decoded));
        }
        continue;
      }

      rawAttributes.add(new RawAttribute(type, null, null, value));
      if (type == MESSAGE_AUTHENTICATOR_TYPE) {
        if (value.length != 16) {
          throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_MESSAGE_AUTHENTICATOR, "Invalid message-authenticator length");
        }
        messageAuthenticator = new MessageAuthenticator(valueOffset, value);
        continue;
      }
      if (type == USER_PASSWORD_TYPE) {
        userPassword = value;
      }

      String name = dictionary.standardName(type).orElse("Attr-" + type);
      RadiusDictionary.AttributeFormat format = dictionary.standardDefinition(type)
          .map(RadiusDictionary.StandardAttribute::format)
          .orElse(RadiusDictionary.AttributeFormat.STRING);
      String decoded = decodeValue(value, format);
      attrs.put(name, decoded);
      attributes.add(new RadiusAttribute(name, decoded));
    }

    String tenantId = attrs.get("Attr-250");
    String username = attrs.get("User-Name");
    String sessionId = attrs.get("Acct-Session-Id");
    String nasIp = attrs.get("NAS-IP-Address");
    String nasIdentifier = attrs.get("NAS-Identifier");
    if (tenantId == null || username == null || sessionId == null || nasIp == null) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.MISSING_REQUIRED_ATTRIBUTE, "Missing required attributes");
    }

    char[] secretChars = sharedSecretProvider.lookupSecret(tenantId, nasIp, nasIdentifier, remoteAddress)
        .orElseThrow(() -> new RadiusPacketValidationException(
            RadiusPacketValidationException.Reason.MISSING_SHARED_SECRET, "No shared secret configured"));
    byte[] sharedSecret = RadiusCrypto.secretBytes(secretChars);
    RadiusCrypto.clear(secretChars);

    try {
      if (validationPolicy.requireMessageAuthenticator() && messageAuthenticator == null
          && (code == RadiusCode.ACCESS_REQUEST || code == RadiusCode.ACCOUNTING_REQUEST)) {
        throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_MESSAGE_AUTHENTICATOR,
            "Missing message-authenticator attribute");
      }
      if (messageAuthenticator != null) {
        if (!validateMessageAuthenticator(payload, length, messageAuthenticator, sharedSecret)) {
          throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_MESSAGE_AUTHENTICATOR,
              "Message-authenticator verification failed");
        }
      }
      if (code == RadiusCode.ACCOUNTING_REQUEST) {
        if (!validateAccountingAuthenticator(payload, length, requestAuthenticator, sharedSecret)) {
          throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_AUTHENTICATOR,
              "Accounting request authenticator invalid");
        }
      }

      if (!replayCache.recordIfFirst(replayKey(remoteAddress, identifier, requestAuthenticator), clock.instant())) {
        throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.REPLAY_DETECTED, "Replay detected");
      }

      if (code == RadiusCode.ACCOUNTING_REQUEST) {
        InterimUpdate interim = new InterimUpdate(
            tenantId,
            sessionId,
            username,
            nasIp,
            attrs.get("Calling-Station-Id"),
            Instant.now(),
            Long.parseLong(attrs.getOrDefault("Acct-Input-Octets", "0")),
            Long.parseLong(attrs.getOrDefault("Acct-Output-Octets", "0")),
            Integer.parseInt(attrs.getOrDefault("Acct-Interim-Interval", "300")));
        return new Decoded(DecodedType.ACCOUNTING, identifier, null, interim,
            new RequestContext(code, identifier, requestAuthenticator, sharedSecret, messageAuthenticator != null, remoteAddress));
      }

      String clearPassword = null;
      if (userPassword != null && userPassword.length > 0) {
        clearPassword = RadiusCrypto.decodeUserPassword(userPassword, requestAuthenticator, sharedSecret);
      }
      AccessRequest access = AccessRequest.builder()
          .tenantId(tenantId)
          .username(username)
          .sessionId(sessionId)
          .nasIp(nasIp)
          .nasIdentifier(nasIdentifier)
          .macAddress(attrs.get("Calling-Station-Id"))
          .framedIpAddress(attrs.get("Framed-IP-Address"))
          .nasPort(attrs.get("NAS-Port"))
          .nasPortId(attrs.get("NAS-Port-Id"))
          .interimIntervalSeconds(Integer.parseInt(attrs.getOrDefault("Acct-Interim-Interval", "300")))
          .userPassword(clearPassword)
          .attributes(attributes)
          .build();
      return new Decoded(DecodedType.ACCESS, identifier, access, null,
          new RequestContext(code, identifier, requestAuthenticator, sharedSecret, messageAuthenticator != null, remoteAddress));
    } catch (RuntimeException ex) {
      RadiusCrypto.clear(sharedSecret);
      throw ex;
    }
  }

  public byte[] encodeResponse(Decoded decoded, RadiusPacket response) {
    RequestContext context = decoded.context();
    byte[] packet = encodePacket(response, context.requestAuthenticator(), context.sharedSecret(), false);
    if (decoded.context().messageAuthenticatorPresent() && validationPolicy.includeMessageAuthenticatorInResponses()) {
      return withMessageAuthenticator(packet, context.sharedSecret());
    }
    return packet;
  }

  public byte[] encodeRequest(RadiusPacket request, byte[] sharedSecret) {
    return encodePacket(request, RadiusCrypto.randomAuthenticator(), sharedSecret, true);
  }

  public RadiusPacket decodeResponse(byte[] payload, RadiusPacket request, byte[] requestAuthenticator, byte[] sharedSecret) {
    if (payload.length < HEADER_SIZE) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_LENGTH, "payload too short");
    }
    ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
    RadiusCode code = RadiusCode.fromCode(Byte.toUnsignedInt(buffer.get()));
    int identifier = Byte.toUnsignedInt(buffer.get());
    int length = Short.toUnsignedInt(buffer.getShort());
    if (identifier != request.getIdentifier()) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_AUTHENTICATOR, "Identifier mismatch");
    }
    if (length < HEADER_SIZE || length > payload.length) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_LENGTH, "Invalid response length");
    }
    byte[] authenticator = new byte[16];
    buffer.get(authenticator);
    if (!validateResponseAuthenticator(payload, length, requestAuthenticator, sharedSecret, authenticator)) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_AUTHENTICATOR, "Invalid response authenticator");
    }
    buffer.position(HEADER_SIZE);
    List<RadiusAttribute> attributes = new ArrayList<>();
    while (buffer.position() < length) {
      int type = Byte.toUnsignedInt(buffer.get());
      int attrLen = Byte.toUnsignedInt(buffer.get());
      if (attrLen < 2 || buffer.position() + attrLen - 2 > length) {
        throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_LENGTH, "invalid attr length");
      }
      byte[] value = new byte[attrLen - 2];
      buffer.get(value);
      if (type == RadiusDictionary.VENDOR_SPECIFIC_TYPE) {
        RawAttribute vendorAttr = decodeVendor(value);
        if (vendorAttr != null) {
          String name = dictionary.vendorName(vendorAttr.vendorId(), vendorAttr.vendorType())
              .orElse("VSA-" + vendorAttr.vendorId() + "-" + vendorAttr.vendorType());
          String decoded = decodeValue(vendorAttr.value(), dictionary.vendorDefinition(name).map(RadiusDictionary.VendorAttribute::format).orElse(RadiusDictionary.AttributeFormat.STRING));
          attributes.add(new RadiusAttribute(name, decoded));
        }
        continue;
      }
      String name = dictionary.standardName(type).orElse("Attr-" + type);
      RadiusDictionary.AttributeFormat format = dictionary.standardDefinition(type)
          .map(RadiusDictionary.StandardAttribute::format)
          .orElse(RadiusDictionary.AttributeFormat.STRING);
      attributes.add(new RadiusAttribute(name, decodeValue(value, format)));
    }
    return new RadiusPacket(code, identifier, attributes);
  }

  private byte[] encodePacket(RadiusPacket packet, byte[] requestAuthenticator, byte[] sharedSecret, boolean includeMessageAuthenticator) {
    List<byte[]> encodedAttributes = new ArrayList<>();
    int length = HEADER_SIZE;
    for (RadiusAttribute attribute : packet.getAttributes()) {
      EncodedAttribute encoded = encodeAttribute(attribute, requestAuthenticator, sharedSecret);
      encodedAttributes.add(encoded.bytes());
      length += encoded.bytes().length;
    }
    if (includeMessageAuthenticator) {
      byte[] placeholder = new byte[18];
      placeholder[0] = (byte) MESSAGE_AUTHENTICATOR_TYPE;
      placeholder[1] = (byte) 18;
      encodedAttributes.add(placeholder);
      length += placeholder.length;
    }

    ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) packet.getCode().getCode());
    buffer.put((byte) packet.getIdentifier());
    buffer.putShort((short) length);
    buffer.put(requestAuthenticator);
    for (byte[] encoded : encodedAttributes) {
      buffer.put(encoded);
    }
    byte[] raw = buffer.array();
    byte[] responseAuthenticator = RadiusCrypto.md5(raw, sharedSecret);
    System.arraycopy(responseAuthenticator, 0, raw, 4, 16);
    if (includeMessageAuthenticator) {
      raw = withMessageAuthenticator(raw, sharedSecret);
    }
    return raw;
  }

  private byte[] withMessageAuthenticator(byte[] packet, byte[] sharedSecret) {
    byte[] copy = packet.clone();
    int offset = findMessageAuthenticatorOffset(copy);
    if (offset < 0) {
      return packet;
    }
    for (int i = 0; i < 16; i++) {
      copy[offset + i] = 0;
    }
    byte[] hmac = RadiusCrypto.hmacMd5(sharedSecret, copy);
    System.arraycopy(hmac, 0, packet, offset, 16);
    return packet;
  }

  private EncodedAttribute encodeAttribute(RadiusAttribute attribute, byte[] requestAuthenticator, byte[] sharedSecret) {
    String name = attribute.getName();
    String value = attribute.getValue();
    if (dictionary.vendorDefinition(name).isPresent()) {
      RadiusDictionary.VendorAttribute vendor = dictionary.vendorDefinition(name).get();
      byte[] encodedValue = encodeValue(value, vendor.format());
      byte[] vendorBlock = encodeVendorAttribute(vendor.vendorId(), vendor.vendorType(), encodedValue);
      ByteBuffer buffer = ByteBuffer.allocate(2 + vendorBlock.length).order(ByteOrder.BIG_ENDIAN);
      buffer.put((byte) RadiusDictionary.VENDOR_SPECIFIC_TYPE);
      buffer.put((byte) (2 + vendorBlock.length));
      buffer.put(vendorBlock);
      return new EncodedAttribute(buffer.array());
    }
    int type = dictionary.standardDefinition(name)
        .map(RadiusDictionary.StandardAttribute::type)
        .orElseGet(() -> parseAttributeType(name));
    byte[] encodedValue = encodeValue(value, dictionary.standardDefinition(type)
        .map(RadiusDictionary.StandardAttribute::format)
        .orElse(RadiusDictionary.AttributeFormat.STRING));
    if (type == USER_PASSWORD_TYPE) {
      encodedValue = RadiusCrypto.encodeUserPassword(value, requestAuthenticator, sharedSecret);
    }
    ByteBuffer buffer = ByteBuffer.allocate(2 + encodedValue.length).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) type);
    buffer.put((byte) (2 + encodedValue.length));
    buffer.put(encodedValue);
    return new EncodedAttribute(buffer.array());
  }

  private int parseAttributeType(String name) {
    if (name.startsWith("Attr-")) {
      return Integer.parseInt(name.substring(5));
    }
    throw new IllegalArgumentException("Unknown attribute: " + name);
  }

  private static byte[] encodeValue(String value, RadiusDictionary.AttributeFormat format) {
    if (value == null) {
      return new byte[0];
    }
    return switch (format) {
      case INTEGER -> ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(Integer.parseInt(value)).array();
      case IP_ADDRESS -> InetAddressValue.encode(value);
      case OCTETS -> Base64.getDecoder().decode(value);
      case STRING -> value.getBytes(StandardCharsets.UTF_8);
    };
  }

  private static String decodeValue(byte[] value, RadiusDictionary.AttributeFormat format) {
    return switch (format) {
      case INTEGER -> Integer.toUnsignedString(ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getInt());
      case IP_ADDRESS -> InetAddressValue.decode(value);
      case OCTETS -> Base64.getEncoder().encodeToString(value);
      case STRING -> new String(value, StandardCharsets.UTF_8).trim();
    };
  }

  private RawAttribute decodeVendor(byte[] value) {
    if (value.length < 6) {
      return null;
    }
    ByteBuffer vsa = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN);
    int vendorId = vsa.getInt();
    int vendorType = Byte.toUnsignedInt(vsa.get());
    int vendorLength = Byte.toUnsignedInt(vsa.get());
    if (vendorLength < 2 || vendorLength - 2 > vsa.remaining()) {
      throw new RadiusPacketValidationException(RadiusPacketValidationException.Reason.INVALID_LENGTH, "invalid vendor attr length");
    }
    byte[] inner = new byte[vendorLength - 2];
    vsa.get(inner);
    return new RawAttribute(RadiusDictionary.VENDOR_SPECIFIC_TYPE, vendorId, vendorType, inner);
  }

  private static byte[] encodeVendorAttribute(int vendorId, int vendorType, byte[] value) {
    ByteBuffer buffer = ByteBuffer.allocate(6 + value.length).order(ByteOrder.BIG_ENDIAN);
    buffer.putInt(vendorId);
    buffer.put((byte) vendorType);
    buffer.put((byte) (value.length + 2));
    buffer.put(value);
    return buffer.array();
  }

  private static boolean validateAccountingAuthenticator(byte[] payload, int length, byte[] requestAuthenticator, byte[] sharedSecret) {
    byte[] copy = new byte[length];
    System.arraycopy(payload, 0, copy, 0, length);
    for (int i = 0; i < 16; i++) {
      copy[4 + i] = 0;
    }
    byte[] expected = RadiusCrypto.md5(copy, sharedSecret);
    return MessageDigestEquals.constantTimeEquals(expected, requestAuthenticator);
  }

  private static boolean validateResponseAuthenticator(byte[] payload,
                                                       int length,
                                                       byte[] requestAuthenticator,
                                                       byte[] sharedSecret,
                                                       byte[] responseAuthenticator) {
    byte[] copy = new byte[length];
    System.arraycopy(payload, 0, copy, 0, length);
    System.arraycopy(requestAuthenticator, 0, copy, 4, 16);
    byte[] expected = RadiusCrypto.md5(copy, sharedSecret);
    return MessageDigestEquals.constantTimeEquals(expected, responseAuthenticator);
  }

  private static boolean validateMessageAuthenticator(byte[] payload,
                                                      int length,
                                                      MessageAuthenticator messageAuthenticator,
                                                      byte[] sharedSecret) {
    byte[] copy = new byte[length];
    System.arraycopy(payload, 0, copy, 0, length);
    for (int i = 0; i < 16; i++) {
      copy[messageAuthenticator.offset + i] = 0;
    }
    byte[] expected = RadiusCrypto.hmacMd5(sharedSecret, copy);
    return MessageDigestEquals.constantTimeEquals(expected, messageAuthenticator.value);
  }

  private static String replayKey(InetSocketAddress remoteAddress, int identifier, byte[] authenticator) {
    return remoteAddress.getAddress().getHostAddress() + ":" + identifier + ":" + Base64.getEncoder().encodeToString(authenticator);
  }

  private static int findMessageAuthenticatorOffset(byte[] packet) {
    ByteBuffer buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN);
    int length = Short.toUnsignedInt(buffer.getShort(2));
    buffer.position(HEADER_SIZE);
    while (buffer.position() < length) {
      int type = Byte.toUnsignedInt(buffer.get());
      int attrLen = Byte.toUnsignedInt(buffer.get());
      if (attrLen < 2 || buffer.position() + attrLen - 2 > length) {
        return -1;
      }
      if (type == MESSAGE_AUTHENTICATOR_TYPE) {
        return buffer.position();
      }
      buffer.position(buffer.position() + attrLen - 2);
    }
    return -1;
  }

  private record MessageAuthenticator(int offset, byte[] value) {
  }

  private record EncodedAttribute(byte[] bytes) {
  }

  private static final class InetAddressValue {
    private InetAddressValue() {
    }

    static byte[] encode(String value) {
      try {
        InetAddress address = InetAddress.getByName(value);
        return address.getAddress();
      } catch (Exception ex) {
        throw new IllegalArgumentException("Invalid IP address: " + value, ex);
      }
    }

    static String decode(byte[] value) {
      try {
        return InetAddress.getByAddress(value).getHostAddress();
      } catch (Exception ex) {
        return new String(value, StandardCharsets.UTF_8).trim();
      }
    }
  }

  private static final class MessageDigestEquals {
    private MessageDigestEquals() {
    }

    static boolean constantTimeEquals(byte[] left, byte[] right) {
      if (left.length != right.length) {
        return false;
      }
      int result = 0;
      for (int i = 0; i < left.length; i++) {
        result |= left[i] ^ right[i];
      }
      return result == 0;
    }
  }
}
