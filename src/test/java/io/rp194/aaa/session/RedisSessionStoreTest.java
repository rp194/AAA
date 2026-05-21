package io.rp194.aaa.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RedisSessionStoreTest {
  @Test
  void storesSessionWithExpectedKeyAndTtl() {
    FakeRedis redis = new FakeRedis();
    RedisSessionStore store = new RedisSessionStore(redis);

    SessionRecord record = new SessionRecord("tenant-a", "sess-1", "alice", "10.0.0.1", "aa:bb",
        Instant.ofEpochSecond(100), Instant.ofEpochSecond(100), 1, 2, 60);
    store.upsert(record);

    assertEquals("session:tenant-a:sess-1", redis.lastExpireKey);
    assertEquals(60, redis.lastExpireSeconds);
    Optional<SessionRecord> loaded = store.find("tenant-a", "sess-1");
    assertTrue(loaded.isPresent());
    assertEquals(1L, loaded.get().getInputOctets());
  }

  @Test
  void incrementUsesAtomicScriptAndRefreshesExpiration() {
    FakeRedis redis = new FakeRedis();
    RedisSessionStore store = new RedisSessionStore(redis);

    store.incrementCounters("tenant-a", "sess-1", 10, 20, Instant.ofEpochSecond(250), 30);

    assertEquals(List.of("session:tenant-a:sess-1"), redis.evalKeys);
    assertEquals("10", redis.evalArgs.get(0));
    assertEquals("20", redis.evalArgs.get(1));
    assertEquals("250", redis.evalArgs.get(2));
    assertEquals("30", redis.evalArgs.get(3));
  }

  private static final class FakeRedis implements RedisSessionStore.RedisSessionCommands {
    private final Map<String, Map<String, String>> hashes = new HashMap<>();
    private String lastExpireKey;
    private int lastExpireSeconds;
    private List<String> evalKeys = List.of();
    private List<String> evalArgs = List.of();

    @Override
    public void hset(String key, Map<String, String> fields) {
      hashes.put(key, new HashMap<>(fields));
    }

    @Override
    public Map<String, String> hgetAll(String key) {
      return hashes.getOrDefault(key, Map.of());
    }

    @Override
    public void expire(String key, int ttlSeconds) {
      this.lastExpireKey = key;
      this.lastExpireSeconds = ttlSeconds;
    }

    @Override
    public void del(String key) {
      hashes.remove(key);
    }

    @Override
    public Object eval(String luaScript, List<String> keys, List<String> args) {
      this.evalKeys = keys;
      this.evalArgs = args;
      return 1L;
    }

    @Override
    public List<String> keys(String pattern) {
      List<String> result = new ArrayList<>();
      String regexPattern = pattern.replace("*", ".*");
      for (String key : hashes.keySet()) {
        if (key.matches(regexPattern)) {
          result.add(key);
        }
      }
      return result;
    }
  }
}
