package io.rp194.aaa.profile;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryUserProfileStore implements UserProfileStore {
  private final Map<String, UserProfile> profiles = new ConcurrentHashMap<>();

  @Override
  public Optional<UserProfile> findProfile(String tenantId, String username) {
    return Optional.ofNullable(profiles.get(key(tenantId, username)));
  }

  @Override
  public void upsert(UserProfile profile) {
    Objects.requireNonNull(profile, "profile");
    profiles.put(key(profile.getTenantId(), profile.getUsername()), profile);
  }

  private static String key(String tenantId, String username) {
    return tenantId + "::" + username;
  }
}
