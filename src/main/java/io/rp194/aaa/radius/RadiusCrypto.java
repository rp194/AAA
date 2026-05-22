package io.rp194.aaa.radius;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class RadiusCrypto {
  private static final SecureRandom RANDOM = new SecureRandom();

  private RadiusCrypto() {
  }

  static byte[] randomAuthenticator() {
    byte[] auth = new byte[16];
    RANDOM.nextBytes(auth);
    return auth;
  }

  static byte[] md5(byte[]... parts) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      for (byte[] part : parts) {
        digest.update(part);
      }
      return digest.digest();
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("MD5 unavailable", ex);
    }
  }

  static byte[] hmacMd5(byte[] key, byte[] message) {
    try {
      Mac mac = Mac.getInstance("HmacMD5");
      mac.init(new SecretKeySpec(key, "HmacMD5"));
      return mac.doFinal(message);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("HmacMD5 unavailable", ex);
    }
  }

  static byte[] encodeUserPassword(String password, byte[] requestAuthenticator, byte[] secret) {
    byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
    int paddedLength = ((passwordBytes.length + 15) / 16) * 16;
    byte[] padded = Arrays.copyOf(passwordBytes, paddedLength);
    byte[] result = new byte[paddedLength];
    byte[] last = requestAuthenticator;
    for (int offset = 0; offset < paddedLength; offset += 16) {
      byte[] hash = md5(secret, last);
      for (int i = 0; i < 16; i++) {
        result[offset + i] = (byte) (padded[offset + i] ^ hash[i]);
      }
      last = Arrays.copyOfRange(result, offset, offset + 16);
    }
    return result;
  }

  static String decodeUserPassword(byte[] encoded, byte[] requestAuthenticator, byte[] secret) {
    byte[] result = new byte[encoded.length];
    byte[] last = requestAuthenticator;
    for (int offset = 0; offset < encoded.length; offset += 16) {
      byte[] hash = md5(secret, last);
      for (int i = 0; i < 16 && offset + i < encoded.length; i++) {
        result[offset + i] = (byte) (encoded[offset + i] ^ hash[i]);
      }
      last = Arrays.copyOfRange(encoded, offset, offset + 16);
    }
    int len = result.length;
    while (len > 0 && result[len - 1] == 0) {
      len--;
    }
    return new String(result, 0, len, StandardCharsets.UTF_8);
  }

  static byte[] secretBytes(char[] secretChars) {
    CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
    ByteBuffer buffer = ByteBuffer.allocate((int) (secretChars.length * encoder.maxBytesPerChar()) + 1);
    CharBuffer charBuffer = CharBuffer.wrap(secretChars);
    encoder.encode(charBuffer, buffer, true);
    encoder.flush(buffer);
    buffer.flip();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return bytes;
  }

  static void clear(byte[] bytes) {
    if (bytes != null) {
      Arrays.fill(bytes, (byte) 0);
    }
  }

  static void clear(char[] chars) {
    if (chars != null) {
      Arrays.fill(chars, '\0');
    }
  }
}
