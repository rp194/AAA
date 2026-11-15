package io.rp194.aaa.transport;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public final class TransportMetrics {
  private final AtomicLong processed = new AtomicLong();
  private final AtomicLong dropped = new AtomicLong();
  private final AtomicLong lastQueueDepth = new AtomicLong();
  private final AtomicLong lastQueueCapacity = new AtomicLong();
  private final AtomicLong maxQueueDepth = new AtomicLong();
  private final AtomicLong[] bucketsMicros;

  public TransportMetrics() {
    this.bucketsMicros = new AtomicLong[11];
    for (int i = 0; i < bucketsMicros.length; i++) {
      bucketsMicros[i] = new AtomicLong();
    }
  }

  public void recordProcessed(long micros) {
    processed.incrementAndGet();
    bucketsMicros[bucket(micros)].incrementAndGet();
  }

  public void recordDropped() { dropped.incrementAndGet(); }
  public void recordQueueDepth(int depth, int capacity) {
    lastQueueDepth.set(depth);
    lastQueueCapacity.set(capacity);
    maxQueueDepth.accumulateAndGet(depth, Math::max);
  }
  public long processedCount() { return processed.get(); }
  public long droppedCount() { return dropped.get(); }
  public long lastQueueDepth() { return lastQueueDepth.get(); }
  public long lastQueueCapacity() { return lastQueueCapacity.get(); }
  public long maxQueueDepth() { return maxQueueDepth.get(); }

  public long p95UpperMicros() { return percentileUpper(95); }
  public long p99UpperMicros() { return percentileUpper(99); }

  private long percentileUpper(int p) {
    long total = processed.get();
    if (total == 0) return 0;
    long threshold = (long) Math.ceil(total * (p / 100.0));
    long seen = 0;
    long[] uppers = {100,250,500,1000,2000,5000,10000,20000,50000,100000,Long.MAX_VALUE};
    for (int i = 0; i < bucketsMicros.length; i++) {
      seen += bucketsMicros[i].get();
      if (seen >= threshold) return uppers[i];
    }
    return Long.MAX_VALUE;
  }

  private int bucket(long micros) {
    long[] uppers = {100,250,500,1000,2000,5000,10000,20000,50000,100000};
    for (int i = 0; i < uppers.length; i++) if (micros <= uppers[i]) return i;
    return 10;
  }

  @Override
  public String toString() {
    return "TransportMetrics{" + "processed=" + processed + ", dropped=" + dropped
        + ", queueDepth=" + lastQueueDepth + "/" + lastQueueCapacity + ", maxQueueDepth=" + maxQueueDepth
        + ", p95<=" + p95UpperMicros() + "us, p99<=" + p99UpperMicros() + "us}";
  }
}
