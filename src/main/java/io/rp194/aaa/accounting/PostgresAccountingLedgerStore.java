package io.rp194.aaa.accounting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PostgresAccountingLedgerStore implements AccountingLedgerStore {
  private final EntityManagerFactory entityManagerFactory;
  private final int batchSize;

  public PostgresAccountingLedgerStore(EntityManagerFactory entityManagerFactory, int batchSize) {
    this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entityManagerFactory");
    this.batchSize = Math.max(1, batchSize);
  }

  @Override
  public void record(AccountingUpdate update) {
    recordBatch(List.of(update));
  }

  public void recordBatch(List<AccountingUpdate> updates) {
    if (updates == null || updates.isEmpty()) {
      return;
    }

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      int pending = 0;
      for (AccountingUpdate update : updates) {
        entityManager.merge(AccountingRowEntity.from(update));
        pending++;
        if (pending >= batchSize) {
          entityManager.flush();
          entityManager.clear();
          pending = 0;
        }
      }
      entityManager.getTransaction().commit();
    } catch (RuntimeException ex) {
      if (entityManager.getTransaction().isActive()) {
        entityManager.getTransaction().rollback();
      }
      throw new IllegalStateException("Failed to persist accounting updates", ex);
    } finally {
      entityManager.close();
    }
  }

  @Override
  public List<AccountingUpdate> all() {
    return Collections.emptyList();
  }

  @Entity
  @Table(name = "accounting_ledger")
  @IdClass(AccountingRowId.class)
  public static class AccountingRowEntity {
    @Id
    @Column(name = "tenant_id")
    private String tenantId;
    @Id
    @Column(name = "session_id")
    private String sessionId;
    @Id
    @Column(name = "event_time")
    private Instant eventTime;
    @Column(name = "username")
    private String username;
    @Column(name = "nas_ip")
    private String nasIp;
    @Column(name = "input_octets")
    private long inputOctets;
    @Column(name = "output_octets")
    private long outputOctets;

    public static AccountingRowEntity from(AccountingUpdate update) {
      AccountingRowEntity row = new AccountingRowEntity();
      row.tenantId = update.getTenantId();
      row.sessionId = update.getSessionId();
      row.eventTime = update.getEventTime();
      row.username = update.getUsername();
      row.nasIp = update.getNasIp();
      row.inputOctets = update.getInputOctets();
      row.outputOctets = update.getOutputOctets();
      return row;
    }
  }

  public static final class AccountingRowId implements Serializable {
    public String tenantId;
    public String sessionId;
    public Instant eventTime;

    public AccountingRowId() {
    }
  }
}
