package io.rp194.aaa.transport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rp194.aaa.accounting.AccountingService;
import io.rp194.aaa.accounting.AsyncLedgerWriter;
import io.rp194.aaa.accounting.InMemoryAccountingLedgerStore;
import io.rp194.aaa.device.InMemoryDeviceProfileRepository;
import io.rp194.aaa.profile.InMemoryUserProfileStore;
import io.rp194.aaa.server.RadiusAccessHandler;
import io.rp194.aaa.session.InMemorySessionStore;
import io.rp194.aaa.vendor.DefaultVendorMapperRegistry;
import io.rp194.aaa.vendor.GenericVendorMapper;
import io.rp194.aaa.vendor.InMemoryTemplateRepository;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AccountingBurstLoadTest {
  @Test
  void reportsP95P99DuringAccountingBurst() throws Exception {
    RadiusRequestRouter router = new RadiusRequestRouter(
        new RadiusAccessHandler(new InMemoryDeviceProfileRepository(), new InMemoryUserProfileStore(),
            new DefaultVendorMapperRegistry(new GenericVendorMapper(), new GenericVendorMapper(), new GenericVendorMapper(), new InMemoryTemplateRepository()), new InMemorySessionStore(), Clock.systemUTC()),
        new AccountingService(new InMemorySessionStore(), new AsyncLedgerWriter(new InMemoryAccountingLedgerStore(), 1), Clock.systemUTC()));
    RadiusPacketCodec codec = new RadiusPacketCodec();
    TransportMetrics metrics = new TransportMetrics();
    WorkerPool workers = new WorkerPool(4, 256, OverloadPolicy.DROP, false);

    int n = 2000;
    CountDownLatch done = new CountDownLatch(n);
    for (int i = 0; i < n; i++) {
      byte[] p = accountingPacket("s" + i, "1", "1", i % 255);
      boolean accepted = workers.submit(() -> {
        long st = System.nanoTime();
        router.route(codec.decode(p));
        metrics.recordProcessed((System.nanoTime() - st) / 1000);
        done.countDown();
      });
      if (!accepted) {
        metrics.recordDropped();
        done.countDown();
      }
    }
    assertTrue(done.await(10, TimeUnit.SECONDS));
    workers.shutdown();
    assertTrue(metrics.p99UpperMicros() > 0);
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
      b.put((byte) ids.get(a[0])).put((byte) (2 + v.length)).put(v);
    }
    return b.array();
  }

}
