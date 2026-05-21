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
  private final EntityManagerFactory entityManagerFactory;

  public PostgresDeviceProfileRepository(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entityManagerFactory");
  }

  @Override
  public Optional<DeviceProfile> findByNas(String tenantId, String nasIp, String nasIdentifier) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaBuilder cb = entityManager.getCriteriaBuilder();
      CriteriaQuery<DeviceProfileEntity> cq = cb.createQuery(DeviceProfileEntity.class);
      Root<DeviceProfileEntity> root = cq.from(DeviceProfileEntity.class);
      cq.select(root).where(
          cb.equal(root.get("tenantId"), tenantId),
          cb.equal(root.get("nasIp"), nasIp),
          cb.equal(root.get("nasIdentifier"), nasIdentifier));

      List<DeviceProfileEntity> result = entityManager.createQuery(cq).setMaxResults(1).getResultList();
      if (result.isEmpty()) {
        return Optional.empty();
      }
      DeviceProfileEntity e = result.get(0);
      return Optional.of(new DeviceProfile(e.tenantId, e.nasIp, e.nasIdentifier, VendorType.valueOf(e.vendorType), e.displayName));
    } finally {
      entityManager.close();
    }
  }

  @Override
  public void register(DeviceProfile profile) {
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

    public static DeviceProfileEntity from(DeviceProfile profile) {
      DeviceProfileEntity entity = new DeviceProfileEntity();
      entity.tenantId = profile.getTenantId();
      entity.nasIp = profile.getNasIp();
      entity.nasIdentifier = profile.getNasIdentifier();
      entity.vendorType = profile.getVendorType().name();
      entity.displayName = profile.getDisplayName();
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
