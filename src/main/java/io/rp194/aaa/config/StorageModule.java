package io.rp194.aaa.config;

import io.rp194.aaa.accounting.AccountingLedgerStore;
import io.rp194.aaa.accounting.InMemoryAccountingLedgerStore;
import io.rp194.aaa.accounting.PostgresAccountingLedgerStore;
import io.rp194.aaa.device.DeviceProfileRepository;
import io.rp194.aaa.device.InMemoryDeviceProfileRepository;
import io.rp194.aaa.device.PostgresDeviceProfileRepository;
import io.rp194.aaa.profile.InMemoryUserProfileStore;
import io.rp194.aaa.profile.PostgresUserProfileStore;
import io.rp194.aaa.profile.UserProfileStore;
import io.rp194.aaa.session.InMemorySessionStore;
import io.rp194.aaa.session.RedisSessionStore;
import io.rp194.aaa.session.SessionStore;
import io.rp194.aaa.session.SessionTtlPolicy;
import jakarta.persistence.EntityManagerFactory;
import java.util.Objects;

public final class StorageModule {
  public enum Profile {
    LOCAL,
    REDIS_POSTGRES
  }

  private final Profile profile;

  public StorageModule(Profile profile) {
    this.profile = Objects.requireNonNull(profile, "profile");
  }

  public SessionStore sessionStore(RedisSessionStore.RedisSessionCommands redisCommands) {
    return switch (profile) {
      case LOCAL -> new InMemorySessionStore();
      case REDIS_POSTGRES -> new RedisSessionStore(redisCommands);
    };
  }

  public SessionStore sessionStore(RedisSessionStore.RedisSessionCommands redisCommands, SessionTtlPolicy ttlPolicy) {
    return switch (profile) {
      case LOCAL -> new InMemorySessionStore();
      case REDIS_POSTGRES -> new RedisSessionStore(redisCommands, ttlPolicy);
    };
  }

  public DeviceProfileRepository deviceProfileRepository(EntityManagerFactory entityManagerFactory) {
    return switch (profile) {
      case LOCAL -> new InMemoryDeviceProfileRepository();
      case REDIS_POSTGRES -> new PostgresDeviceProfileRepository(entityManagerFactory);
    };
  }

  public UserProfileStore userProfileStore(EntityManagerFactory entityManagerFactory) {
    return switch (profile) {
      case LOCAL -> new InMemoryUserProfileStore();
      case REDIS_POSTGRES -> new PostgresUserProfileStore(entityManagerFactory);
    };
  }

  public AccountingLedgerStore accountingLedgerStore(EntityManagerFactory entityManagerFactory) {
    return switch (profile) {
      case LOCAL -> new InMemoryAccountingLedgerStore();
      case REDIS_POSTGRES -> new PostgresAccountingLedgerStore(entityManagerFactory, 500);
    };
  }
}
