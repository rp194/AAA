package io.rp194.aaa.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rp194.aaa.radius.AccessRequest;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RadiusPacketCodecTest {
  private final RadiusPacketCodec codec = new RadiusPacketCodec();

  @Test
  void decodesUnknownAttributesAndVsaFallbacks() {
    byte[] packet = radiusPacket(1, 7,
        attr(1, "u1"),
        attr(4, "192.0.2.10"),
        attr(44, "sess-1"),
        attr(250, "tenant-a"),
        attr(99, "mystery"),
        vsa(555, 1, "opaque"));

    AccessRequest request = codec.decode(packet).accessRequest();

    assertEquals("u1", request.getUsername());
    assertTrue(request.findAttribute("Attr-99").isPresent());
    assertEquals("mystery", request.findAttribute("Attr-99").orElseThrow());
    assertEquals("opaque", request.findAttribute("VSA-555-1").orElseThrow());
  }

  @Test
  void lastDuplicateAttributeWins() {
    byte[] packet = radiusPacket(1, 7,
        attr(1, "u1"),
        attr(1, "u2"),
        attr(4, "192.0.2.10"),
        attr(44, "sess-1"),
        attr(250, "tenant-a"));

    AccessRequest request = codec.decode(packet).accessRequest();

    assertEquals("u2", request.getUsername());
  }

  @Test
  void rejectsUnknownCodes() {
    byte[] packet = radiusPacket(9, 1, attr(1, "u1"));
    assertThrows(IllegalArgumentException.class, () -> codec.decode(packet));
  }

  @Test
  void rejectsMissingRequiredAttributes() {
    byte[] packet = headerOnly(1, 1);
    assertThrows(IllegalArgumentException.class, () -> codec.decode(packet));
  }

  @Test
  void rejectsInvalidLengthHeader() {
    byte[] payload = new byte[10];
    assertThrows(IllegalArgumentException.class, () -> codec.decode(payload));
  }

  @Test
  void rejectsLengthLargerThanPayload() {
    ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) 1).put((byte) 1).putShort((short) 99).put(new byte[16]);
    assertThrows(IllegalArgumentException.class, () -> codec.decode(buffer.array()));
  }

  @Test
  void rejectsTruncatedAttributes() {
    ByteBuffer buffer = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) 1).put((byte) 1).putShort((short) 24).put(new byte[16]);
    buffer.put((byte) 1).put((byte) 10);
    assertThrows(IllegalArgumentException.class, () -> codec.decode(buffer.array()));
  }

  @Test
  void rejectsInvalidAttributeLength() {
    ByteBuffer buffer = ByteBuffer.allocate(22).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) 1).put((byte) 1).putShort((short) 22).put(new byte[16]);
    buffer.put((byte) 1).put((byte) 1);
    assertThrows(IllegalArgumentException.class, () -> codec.decode(buffer.array()));
  }

  @Test
  void rejectsOversizedAttributeLength() {
    ByteBuffer buffer = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) 1).put((byte) 1).putShort((short) 24).put(new byte[16]);
    buffer.put((byte) 1).put((byte) 200);
    assertThrows(IllegalArgumentException.class, () -> codec.decode(buffer.array()));
  }

  private static byte[] headerOnly(int code, int identifier) {
    ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) code).put((byte) identifier).putShort((short) 20).put(new byte[16]);
    return buffer.array();
  }

  private static byte[] radiusPacket(int code, int identifier, Attribute... attrs) {
    int len = 20;
    for (Attribute attr : attrs) {
      len += 2 + attr.value().length;
    }
    ByteBuffer buffer = ByteBuffer.allocate(len).order(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) code).put((byte) identifier).putShort((short) len).put(new byte[16]);
    for (Attribute attr : attrs) {
      buffer.put((byte) attr.type()).put((byte) (2 + attr.value().length)).put(attr.value());
    }
    return buffer.array();
  }

  private static Attribute attr(int type, String value) {
    return new Attribute(type, value.getBytes(StandardCharsets.UTF_8));
  }

  private static Attribute vsa(int vendorId, int vendorType, String value) {
    byte[] inner = value.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.allocate(6 + inner.length).order(ByteOrder.BIG_ENDIAN);
    buffer.putInt(vendorId);
    buffer.put((byte) vendorType);
    buffer.put((byte) (2 + inner.length));
    buffer.put(inner);
    return new Attribute(26, buffer.array());
  }

  private record Attribute(int type, byte[] value) {}
}
