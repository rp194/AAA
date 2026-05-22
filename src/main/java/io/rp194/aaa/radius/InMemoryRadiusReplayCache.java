package io.rp194.aaa.radius;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRadiusReplayCache implements RadiusReplayCache {
  private final Duration ttl;
  private final Map<String, Instant> entries = new ConcurrentHashMap<>();

  public InMemoryRadiusReplayCache(Duration ttl) {
    this.ttl = ttl == null ? Duration.ZERO : ttl;
  }

  @Override
  public boolean recordIfFirst(String key, Instant now) {
    if (ttl.isZero() || ttl.isNegative()) {
      return true;
    }
    entries.entrySet().removeIf(entry -> entry.getValue().plus(ttl).isBefore(now));
    return entries.putIfAbsent(key, now) == null;
  }
}
