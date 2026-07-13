package com.quanlycuahang.erp.auth.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Gan 1 EntityManager rieng vao thread hien tai va bat Hibernate @Filter "tenantFilter" cho dung
 * tenantId - logic nay TACH TU TenantFilter (dung chung, tranh 2 noi cung code nhay cam bao mat nay
 * tu roi nhau theo thoi gian). TenantFilter goi cho MOI HTTP request; ngoai ra bat ky tac vu nao
 * chay tren THREAD KHAC voi request goc (vd @Async — xem InvoiceEmailService) cung PHAI tu goi
 * bind()/unbind() nay o dau/cuoi, vi TenantContext (ThreadLocal) va Hibernate Session KHONG tu ke
 * thua sang thread moi.
 */
@Component
public class TenantSessionBinder {

  private final EntityManagerFactory entityManagerFactory;

  public TenantSessionBinder(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  public EntityManager bind(Long tenantId) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    TransactionSynchronizationManager.bindResource(
        entityManagerFactory, new EntityManagerHolder(entityManager));
    if (tenantId != null) {
      Session session = entityManager.unwrap(Session.class);
      session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }
    return entityManager;
  }

  public void unbind(EntityManager entityManager) {
    TransactionSynchronizationManager.unbindResource(entityManagerFactory);
    EntityManagerFactoryUtils.closeEntityManager(entityManager);
  }
}
