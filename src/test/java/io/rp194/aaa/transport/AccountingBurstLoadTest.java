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
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AccountingBurstLoadTest {
  @Test
  void reportsP95P99DuringAccountingBurst() throws Exception {
    RadiusRequestRouter router = new RadiusRequestRouter(
        new RadiusAccessHandler(new InMemoryDeviceProfileRepository(), new InMemoryUserProfileStore(),
            new DefaultVendorMapperRegistry(), new InMemorySessionStore(), Clock.systemUTC()),
        new AccountingService(new InMemorySessionStore(), new AsyncLedgerWriter(new InMemoryAccountingLedgerStore()), Clock.systemUTC()));
    RadiusPacketCodec codec = new RadiusPacketCodec();
    TransportMetrics metrics = new TransportMetrics();
    WorkerPool workers = new WorkerPool(4, 256, OverloadPolicy.DROP, false);

    int n = 2000;
    CountDownLatch done = new CountDownLatch(n);
    for (int i = 0; i < n; i++) {
      byte[] p = ("type=acct;tenant=t1;user=u1;session=s" + i + ";nasIp=1.1.1.1;in=1;out=1").getBytes();
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
}
