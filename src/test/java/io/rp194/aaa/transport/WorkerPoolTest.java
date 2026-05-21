package io.rp194.aaa.transport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class WorkerPoolTest {

  @Test
  void rejectsWhenQueueIsFull() throws Exception {
    WorkerPool pool = new WorkerPool(1, 1, OverloadPolicy.DROP, false);
    CountDownLatch blocker = new CountDownLatch(1);
    assertTrue(pool.submit(() -> await(blocker)));
    assertTrue(pool.submit(() -> await(blocker)));
    assertFalse(pool.submit(() -> { }));
    blocker.countDown();
    pool.shutdown();
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
