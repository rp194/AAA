package io.rp194.aaa.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RedisSessionStore implements SessionStore {
  private static final String UPDATE_COUNTERS_LUA = """
      local key = KEYS[1]
      redis.call('HINCRBY', key, 'inputOctets', ARGV[1])
      redis.call('HINCRBY', key, 'outputOctets', ARGV[2])
      redis.call('HSET', key, 'lastUpdate', ARGV[3])
      redis.call('EXPIRE', key, ARGV[4])
      return 1
      """;

  private final RedisSessionCommands redis;

  public RedisSessionStore(RedisSessionCommands redis) {
    this.redis = Objects.requireNonNull(redis, "redis");
  }

  @Override
  public void upsert(SessionRecord record) {
    String key = key(record.getTenantId(), record.getSessionId());
    int ttlSeconds = ttlSeconds(record.getInterimIntervalSeconds());
    redis.hset(key, Map.of(
        "tenantId", record.getTenantId(),
        "sessionId", record.getSessionId(),
        "username", record.getUsername(),
        "nasIp", record.getNasIp(),
        "macAddress", record.getMacAddress() == null ? "" : record.getMacAddress(),
        "startTime", Long.toString(record.getStartTime().getEpochSecond()),
        "lastUpdate", Long.toString(record.getLastUpdate().getEpochSecond()),
        "inputOctets", Long.toString(record.getInputOctets()),
        "outputOctets", Long.toString(record.getOutputOctets()),
        "interimIntervalSeconds", Integer.toString(record.getInterimIntervalSeconds())));
    redis.expire(key, ttlSeconds);
  }

  public void incrementCounters(String tenantId,
                                String sessionId,
                                long inputOctetsDelta,
                                long outputOctetsDelta,
                                Instant eventTime,
                                int interimIntervalSeconds) {
    redis.eval(UPDATE_COUNTERS_LUA,
        List.of(key(tenantId, sessionId)),
        List.of(
            Long.toString(inputOctetsDelta),
            Long.toString(outputOctetsDelta),
            Long.toString(eventTime.getEpochSecond()),
            Integer.toString(ttlSeconds(interimIntervalSeconds))));
  }

  @Override
  public Optional<SessionRecord> find(String tenantId, String sessionId) {
    Map<String, String> values = redis.hgetAll(key(tenantId, sessionId));
    if (values.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(new SessionRecord(
        values.get("tenantId"),
        values.get("sessionId"),
        values.get("username"),
        values.get("nasIp"),
        nullable(values.get("macAddress")),
        Instant.ofEpochSecond(Long.parseLong(values.get("startTime"))),
        Instant.ofEpochSecond(Long.parseLong(values.get("lastUpdate"))),
        Long.parseLong(values.get("inputOctets")),
        Long.parseLong(values.get("outputOctets")),
        Integer.parseInt(values.get("interimIntervalSeconds"))));
  }

  @Override
  public List<SessionRecord> findExpired(Instant now) {
    throw new UnsupportedOperationException("Redis-backed expiration is TTL-driven; no scan-based expiration query.");
  }

  @Override
  public void remove(String tenantId, String sessionId) {
    redis.del(key(tenantId, sessionId));
  }

  private static String key(String tenantId, String sessionId) {
    return "session:" + tenantId + ":" + sessionId;
  }

  private static String nullable(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static int ttlSeconds(int interimIntervalSeconds) {
    return Math.max(interimIntervalSeconds, 1);
  }

  public interface RedisSessionCommands {
    void hset(String key, Map<String, String> fields);

    Map<String, String> hgetAll(String key);

    void expire(String key, int ttlSeconds);

    void del(String key);

    Object eval(String luaScript, List<String> keys, List<String> args);
  }
}
