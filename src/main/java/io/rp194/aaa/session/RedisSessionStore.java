package io.rp194.aaa.session;

import java.time.Instant;
import java.util.ArrayList;
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
  private final SessionTtlPolicy ttlPolicy;

  public RedisSessionStore(RedisSessionCommands redis) {
    this(redis, SessionTtlPolicy.defaults());
  }

  public RedisSessionStore(RedisSessionCommands redis, SessionTtlPolicy ttlPolicy) {
    this.redis = Objects.requireNonNull(redis, "redis");
    this.ttlPolicy = Objects.requireNonNull(ttlPolicy, "ttlPolicy");
  }

  @Override
  public void upsert(SessionRecord record) {
    String key = key(record.getTenantId(), record.getSessionId());
    int ttlSeconds = ttlSeconds(record.getInterimIntervalSeconds());
    Map<String, String> fields = new java.util.HashMap<>();
    fields.put("tenantId", record.getTenantId());
    fields.put("sessionId", record.getSessionId());
    fields.put("username", record.getUsername());
    fields.put("nasIp", record.getNasIp());
    fields.put("framedIpAddress", nullToBlank(record.getFramedIpAddress()));
    fields.put("nasPort", nullToBlank(record.getNasPort()));
    fields.put("nasPortId", nullToBlank(record.getNasPortId()));
    fields.put("macAddress", record.getMacAddress() == null ? "" : record.getMacAddress());
    fields.put("startTime", Long.toString(record.getStartTime().getEpochSecond()));
    fields.put("lastUpdate", Long.toString(record.getLastUpdate().getEpochSecond()));
    fields.put("inputOctets", Long.toString(record.getInputOctets()));
    fields.put("outputOctets", Long.toString(record.getOutputOctets()));
    fields.put("interimIntervalSeconds", Integer.toString(record.getInterimIntervalSeconds()));
    redis.hset(key, fields);
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
        nullable(values.get("framedIpAddress")),
        nullable(values.get("nasPort")),
        nullable(values.get("nasPortId")),
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
  public List<SessionRecord> findByTenantAndUsername(String tenantId, String username) {
    List<String> matchingKeys = redis.keys("session:" + tenantId + ":*");
    List<SessionRecord> results = new ArrayList<>();
    for (String key : matchingKeys) {
      Map<String, String> values = redis.hgetAll(key);
      if (!values.isEmpty() && username.equals(values.get("username"))) {
        results.add(new SessionRecord(
            values.get("tenantId"),
            values.get("sessionId"),
            values.get("username"),
            values.get("nasIp"),
            nullable(values.get("framedIpAddress")),
            nullable(values.get("nasPort")),
            nullable(values.get("nasPortId")),
            nullable(values.get("macAddress")),
            Instant.ofEpochSecond(Long.parseLong(values.get("startTime"))),
            Instant.ofEpochSecond(Long.parseLong(values.get("lastUpdate"))),
            Long.parseLong(values.get("inputOctets")),
            Long.parseLong(values.get("outputOctets")),
            Integer.parseInt(values.get("interimIntervalSeconds"))));
      }
    }
    return results;
  }

  @Override
  public List<SessionRecord> findByTenant(String tenantId) {
    List<String> matchingKeys = redis.keys("session:" + tenantId + ":*");
    List<SessionRecord> results = new ArrayList<>();
    for (String key : matchingKeys) {
      Map<String, String> values = redis.hgetAll(key);
      if (!values.isEmpty()) {
        results.add(new SessionRecord(
            values.get("tenantId"),
            values.get("sessionId"),
            values.get("username"),
            values.get("nasIp"),
            nullable(values.get("framedIpAddress")),
            nullable(values.get("nasPort")),
            nullable(values.get("nasPortId")),
            nullable(values.get("macAddress")),
            Instant.ofEpochSecond(Long.parseLong(values.get("startTime"))),
            Instant.ofEpochSecond(Long.parseLong(values.get("lastUpdate"))),
            Long.parseLong(values.get("inputOctets")),
            Long.parseLong(values.get("outputOctets")),
            Integer.parseInt(values.get("interimIntervalSeconds"))));
      }
    }
    return results;
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

  private static String nullToBlank(String value) {
    return value == null ? "" : value;
  }

  private int ttlSeconds(int interimIntervalSeconds) {
    return ttlPolicy.ttlSeconds(interimIntervalSeconds);
  }

  public interface RedisSessionCommands {
    void hset(String key, Map<String, String> fields);

    Map<String, String> hgetAll(String key);

    void expire(String key, int ttlSeconds);

    void del(String key);

    Object eval(String luaScript, List<String> keys, List<String> args);

    List<String> keys(String pattern);
  }
}
