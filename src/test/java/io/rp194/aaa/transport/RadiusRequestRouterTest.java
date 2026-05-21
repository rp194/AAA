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

    var decodedAccess = codec.decode("type=access;id=10;tenant=t1;user=u1;session=s1;nasIp=1.1.1.1".getBytes());
    assertTrue(router.route(decodedAccess).isPresent());
    assertEquals(10, router.route(decodedAccess).orElseThrow().getIdentifier());

    var decodedAcct = codec.decode("type=acct;id=11;tenant=t1;user=u1;session=s1;nasIp=1.1.1.1;in=10;out=12".getBytes());
    assertTrue(router.route(decodedAcct).isEmpty());
  }
}
