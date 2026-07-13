package com.quanlycuahang.erp.common.audit;

import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.system.entity.AuditLog;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi audit cho hanh dong BI CHAN/THAT BAI (vd 1 guard nghiep vu nem exception giua chung) - tach
 * rieng khoi AuditAspect thanh 1 bean/method @Transactional(REQUIRES_NEW) vi ban ghi nay PHAI song
 * sot ngay ca khi transaction cua method dang audit bi rollback do chinh exception do - neu ghi
 * trong CUNG transaction (goi qua "this" trong AuditAspect se khong ap dung duoc
 * proxy @Transactional cua Spring - self-invocation), ban ghi audit se bi rollback theo, mat dau
 * vet dung luc can nhat (phat hien khi rieng soat: truoc day hanh dong bi chan hoan toan khong de
 * lai dau vet).
 */
@Service
class AuditFailureRecorder {

  private final AuditLogRepository auditLogRepository;
  private final UserRepository userRepository;
  private final EntityManager entityManager;

  AuditFailureRecorder(
      AuditLogRepository auditLogRepository,
      UserRepository userRepository,
      EntityManager entityManager) {
    this.auditLogRepository = auditLogRepository;
    this.userRepository = userRepository;
    this.entityManager = entityManager;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
      Long tenantId,
      String action,
      String entityName,
      String before,
      String afterDetail,
      String username) {
    AuditLog auditLog = new AuditLog();
    auditLog.setTenant(entityManager.getReference(Tenant.class, tenantId));
    auditLog.setAction(action);
    auditLog.setEntityName(entityName);
    auditLog.setBefore(before);
    auditLog.setAfter(afterDetail);
    if (username != null) {
      userRepository.findByUsernameAndActiveTrue(username).ifPresent(auditLog::setUser);
    }
    auditLogRepository.save(auditLog);
  }
}
