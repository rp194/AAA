package io.rp194.aaa.device;

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

public final class PostgresDeviceProfileRepository implements DeviceProfileRepository {
  interface DeviceProfileLookup {
    Optional<DeviceProfile> findByNasIdentifier(String tenantId, String nasIdentifier);

    Optional<DeviceProfile> findByNasIp(String tenantId, String nasIp);
  }

  private final EntityManagerFactory entityManagerFactory;
  private final DeviceProfileLookup lookup;

  public PostgresDeviceProfileRepository(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entityManagerFactory");
    this.lookup = new JpaDeviceProfileLookup(entityManagerFactory);
  }

  PostgresDeviceProfileRepository(DeviceProfileLookup lookup) {
    this.entityManagerFactory = null;
    this.lookup = Objects.requireNonNull(lookup, "lookup");
  }

  @Override
  public Optional<DeviceProfile> findByNas(String tenantId, String nasIp, String nasIdentifier) {
    Objects.requireNonNull(tenantId, "tenantId");
    if (nasIdentifier != null && !nasIdentifier.isBlank()) {
      Optional<DeviceProfile> byIdentifier = lookup.findByNasIdentifier(tenantId, nasIdentifier);
      if (byIdentifier.isPresent()) {
        return byIdentifier;
      }
    }
    if (nasIp == null || nasIp.isBlank()) {
      return Optional.empty();
    }
    return lookup.findByNasIp(tenantId, nasIp);
  }

  @Override
  public void register(DeviceProfile profile) {
    if (entityManagerFactory == null) {
      throw new UnsupportedOperationException("No EntityManagerFactory configured for register()");
    }
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      entityManager.merge(DeviceProfileEntity.from(profile));
      entityManager.getTransaction().commit();
    } catch (RuntimeException ex) {
      if (entityManager.getTransaction().isActive()) {
        entityManager.getTransaction().rollback();
      }
      throw new IllegalStateException("Failed to register device profile", ex);
    } finally {
      entityManager.close();
    }
  }

  private static final class JpaDeviceProfileLookup implements DeviceProfileLookup {
    private final EntityManagerFactory entityManagerFactory;

    private JpaDeviceProfileLookup(EntityManagerFactory entityManagerFactory) {
      this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entityManagerFactory");
    }

    @Override
    public Optional<DeviceProfile> findByNasIdentifier(String tenantId, String nasIdentifier) {
      if (nasIdentifier == null || nasIdentifier.isBlank()) {
        return Optional.empty();
      }
      return query(profileRoot -> profileRoot.get("nasIdentifier"), tenantId, nasIdentifier);
    }

    @Override
    public Optional<DeviceProfile> findByNasIp(String tenantId, String nasIp) {
      if (nasIp == null || nasIp.isBlank()) {
        return Optional.empty();
      }
      return query(profileRoot -> profileRoot.get("nasIp"), tenantId, nasIp);
    }

    private Optional<DeviceProfile> query(java.util.function.Function<Root<DeviceProfileEntity>, jakarta.persistence.criteria.Path<String>> field,
                                          String tenantId,
                                          String value) {
      EntityManager entityManager = entityManagerFactory.createEntityManager();
      try {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DeviceProfileEntity> cq = cb.createQuery(DeviceProfileEntity.class);
        Root<DeviceProfileEntity> root = cq.from(DeviceProfileEntity.class);
        cq.select(root).where(
            cb.equal(root.get("tenantId"), tenantId),
            cb.equal(field.apply(root), value));

        List<DeviceProfileEntity> result = entityManager.createQuery(cq).setMaxResults(1).getResultList();
        if (result.isEmpty()) {
          return Optional.empty();
        }
        DeviceProfileEntity e = result.get(0);
        return Optional.of(new DeviceProfile(
            e.tenantId,
            e.nasIp,
            e.nasIdentifier,
            VendorType.valueOf(e.vendorType),
            e.displayName,
            e.sharedSecret));
      } finally {
        entityManager.close();
      }
    }
  }

  @Entity
  @Table(name = "device_profile")
  @IdClass(DeviceProfileId.class)
  public static class DeviceProfileEntity {
    @Id
    @Column(name = "tenant_id")
    private String tenantId;
    @Id
    @Column(name = "nas_ip")
    private String nasIp;
    @Id
    @Column(name = "nas_identifier")
    private String nasIdentifier;
    @Column(name = "vendor_type")
    private String vendorType;
    @Column(name = "display_name")
    private String displayName;
    @Column(name = "shared_secret")
    private String sharedSecret;

    public static DeviceProfileEntity from(DeviceProfile profile) {
      DeviceProfileEntity entity = new DeviceProfileEntity();
      entity.tenantId = profile.getTenantId();
      entity.nasIp = profile.getNasIp();
      entity.nasIdentifier = profile.getNasIdentifier();
      entity.vendorType = profile.getVendorType().name();
      entity.displayName = profile.getDisplayName();
      entity.sharedSecret = profile.getSharedSecret();
      return entity;
    }
  }

  public static final class DeviceProfileId implements Serializable {
    public String tenantId;
    public String nasIp;
    public String nasIdentifier;

    public DeviceProfileId() {
    }
  }
}
