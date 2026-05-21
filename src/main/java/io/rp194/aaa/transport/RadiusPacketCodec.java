package io.rp194.aaa.transport;

import io.rp194.aaa.accounting.InterimUpdate;
import io.rp194.aaa.radius.AccessRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class RadiusPacketCodec {
  public enum DecodedType { ACCESS, ACCOUNTING }

  public record Decoded(DecodedType type, int identifier, AccessRequest accessRequest, InterimUpdate interimUpdate) {}

  public Decoded decode(byte[] payload) {
    // lightweight line protocol for transport boundary tests: type=access;tenant=t1;user=u1;...
    String body = new String(payload, StandardCharsets.UTF_8);
    Map<String, String> kv = parse(body);
    String type = kv.getOrDefault("type", "access");
    if ("acct".equals(type)) {
      InterimUpdate interim = new InterimUpdate(
          kv.get("tenant"), kv.get("session"), kv.get("user"), kv.get("nasIp"), kv.get("mac"),
          Instant.now(), Long.parseLong(kv.getOrDefault("in", "0")), Long.parseLong(kv.getOrDefault("out", "0")),
          Integer.parseInt(kv.getOrDefault("interim", "300")));
      return new Decoded(DecodedType.ACCOUNTING, Integer.parseInt(kv.getOrDefault("id", "0")), null, interim);
    }

    AccessRequest access = AccessRequest.builder()
        .tenantId(kv.get("tenant")).username(kv.get("user")).sessionId(kv.get("session"))
        .nasIp(kv.getOrDefault("nasIp", "0.0.0.0")).nasIdentifier(kv.get("nasId")).macAddress(kv.get("mac"))
        .interimIntervalSeconds(Integer.parseInt(kv.getOrDefault("interim", "300"))).build();
    return new Decoded(DecodedType.ACCESS, Integer.parseInt(kv.getOrDefault("id", "0")), access, null);
  }

  private Map<String, String> parse(String in) {
    Map<String, String> out = new HashMap<>();
    String[] pairs = in.split(";");
    for (String p : pairs) {
      String[] s = p.split("=", 2);
      if (s.length == 2) out.put(s[0], s[1]);
    }
    return out;
  }
}
