package io.rp194.aaa.profile;

import java.util.Optional;

public interface UserProfileStore {
  Optional<UserProfile> findProfile(String tenantId, String username);

  void upsert(UserProfile profile);
}
