package io.rp194.aaa.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rp194.aaa.accounting.AccountingService;
import io.rp194.aaa.accounting.AsyncLedgerWriter;
import io.rp194.aaa.accounting.InMemoryAccountingLedgerStore;
import io.rp194.aaa.device.InMemoryDeviceProfileRepository;
import io.rp194.aaa.profile.InMemoryUserProfileStore;
import io.rp194.aaa.profile.UserProfile;
import io.rp194.aaa.server.RadiusAccessHandler;
import io.rp194.aaa.session.InMemorySessionStore;
import io.rp194.aaa.vendor.DefaultVendorMapperRegistry;
import io.rp194.aaa.vendor.GenericVendorMapper;
import io.rp194.aaa.vendor.InMemoryTemplateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RadiusRequestRouterTest {

  @Test
  void routesAccessAndAccountingToServiceBoundary() {
    InMemoryUserProfileStore profiles = new InMemoryUserProfileStore();
    profiles.upsert(new UserProfile("t1", "u1", "pw", "10M/10M", "1.1.1.1", 0, 1, 1000, 1000, "default"));
    RadiusAccessHandler accessHandler = new RadiusAccessHandler(
        new InMemoryDeviceProfileRepository(),
        profiles,
        new DefaultVendorMapperRegistry(new GenericVendorMapper(), new GenericVendorMapper(), new GenericVendorMapper(), new InMemoryTemplateRepository()),
        new InMemorySessionStore(),
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    AccountingService accountingService = new AccountingService(
        new InMemorySessionStore(),
        new AsyncLedgerWriter(new InMemoryAccountingLedgerStore(), 1),
        Clock.systemUTC());

    RadiusRequestRouter router = new RadiusRequestRouter(accessHandler, accountingService);
    RadiusPacketCodec codec = new RadiusPacketCodec();

    var decodedAccess = codec.decode(accessPacket());
    assertTrue(router.route(decodedAccess).isPresent());
    assertEquals(10, router.route(decodedAccess).orElseThrow().getIdentifier());

    var decodedAcct = codec.decode(accountingPacket("s1", "10", "12", 11));
    assertTrue(router.route(decodedAcct).isEmpty());
  }


  private static byte[] accessPacket() {
    return radiusPacket(1, 10, new String[][] {
        {"User-Name", "u1"},
        {"NAS-IP-Address", "1.1.1.1"},
        {"Acct-Session-Id", "s1"},
        {"Attr-250", "t1"}
    });
  }

  private static byte[] accountingPacket(String sessionId, String in, String out, int identifier) {
    return radiusPacket(4, identifier, new String[][] {
        {"User-Name", "u1"},
        {"NAS-IP-Address", "1.1.1.1"},
        {"Acct-Session-Id", sessionId},
        {"Attr-250", "t1"},
        {"Acct-Input-Octets", in},
        {"Acct-Output-Octets", out}
    });
  }

  private static byte[] radiusPacket(int code, int identifier, String[][] attrs) {
    java.util.Map<String, Integer> ids = java.util.Map.of(
        "User-Name", 1, "NAS-IP-Address", 4, "Acct-Input-Octets", 42, "Acct-Output-Octets", 43,
        "Acct-Session-Id", 44, "Attr-250", 250);
    int len = 20;
    for (String[] a : attrs) len += 2 + a[1].getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(len).order(java.nio.ByteOrder.BIG_ENDIAN);
    b.put((byte) code).put((byte) identifier).putShort((short) len).put(new byte[16]);
    for (String[] a : attrs) {
      byte[] v = a[1].getBytes(java.nio.charset.StandardCharsets.UTF_8);
      b.put(ids.get(a[0]).byteValue()).put((byte) (2 + v.length)).put(v);
    }
    return b.array();
  }

}
