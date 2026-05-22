package io.rp194.aaa.radius;

import java.time.Instant;

public interface RadiusReplayCache {
  boolean recordIfFirst(String key, Instant now);
}
