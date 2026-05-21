package io.rp194.aaa.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PostgresUserProfileStore implements UserProfileStore {
  private final EntityManagerFactory entityManagerFactory;

  public PostgresUserProfileStore(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entityManagerFactory");
  }

  @Override
  public Optional<UserProfile> findProfile(String tenantId, String username) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaBuilder cb = entityManager.getCriteriaBuilder();
      CriteriaQuery<UserProfileEntity> cq = cb.createQuery(UserProfileEntity.class);
      Root<UserProfileEntity> root = cq.from(UserProfileEntity.class);
      cq.select(root)
          .where(cb.equal(root.get("tenantId"), tenantId), cb.equal(root.get("username"), username));
      List<UserProfileEntity> result = entityManager.createQuery(cq).setMaxResults(1).getResultList();
      if (result.isEmpty()) {
        return Optional.empty();
      }
      UserProfileEntity e = result.get(0);
      return Optional.of(new UserProfile(e.tenantId, e.username, e.bandwidthUpKbps, e.bandwidthDownKbps, e.serviceProfile));
    } finally {
      entityManager.close();
    }
  }

  @Override
  public void upsert(UserProfile profile) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      entityManager.merge(UserProfileEntity.from(profile));
      entityManager.getTransaction().commit();
    } catch (RuntimeException ex) {
      if (entityManager.getTransaction().isActive()) {
        entityManager.getTransaction().rollback();
      }
      throw new IllegalStateException("Failed to upsert user profile", ex);
    } finally {
      entityManager.close();
    }
  }

  @Entity
  @Table(name = "user_profile")
  @IdClass(UserProfileId.class)
  public static class UserProfileEntity {
    @Id
    @Column(name = "tenant_id")
    private String tenantId;
    @Id
    @Column(name = "username")
    private String username;
    @Column(name = "bandwidth_up_kbps")
    private int bandwidthUpKbps;
    @Column(name = "bandwidth_down_kbps")
    private int bandwidthDownKbps;
    @Column(name = "service_profile")
    private String serviceProfile;

    public static UserProfileEntity from(UserProfile profile) {
      UserProfileEntity e = new UserProfileEntity();
      e.tenantId = profile.getTenantId();
      e.username = profile.getUsername();
      e.bandwidthUpKbps = profile.getBandwidthUpKbps();
      e.bandwidthDownKbps = profile.getBandwidthDownKbps();
      e.serviceProfile = profile.getServiceProfile();
      return e;
    }
  }

  public static final class UserProfileId implements Serializable {
    public String tenantId;
    public String username;

    public UserProfileId() {
    }
  }
}
