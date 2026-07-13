package com.quanlycuahang.erp.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.system.entity.AuditLog;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Bat quanh moi method co @Audited: ghi lai action, nguoi thuc hien, tham so dau vao (before) va
 * ket qua tra ve (after) duoi dang JSON vao audit_logs (D3). Hanh dong THAT BAI/BI CHAN giua chung
 * cung duoc ghi (action + "_ATTEMPT", qua AuditFailureRecorder o transaction rieng de khong bi
 * rollback theo) - truoc day chi ghi khi thanh cong nen 1 hanh dong pha hoai bi chan hoan toan
 * khong de lai dau vet (phat hien khi rieng soat).
 */
@Aspect
@Component
public class AuditAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

  private final AuditLogRepository auditLogRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final EntityManager entityManager;
  private final AuditFailureRecorder auditFailureRecorder;

  public AuditAspect(
      AuditLogRepository auditLogRepository,
      UserRepository userRepository,
      ObjectMapper objectMapper,
      EntityManager entityManager,
      AuditFailureRecorder auditFailureRecorder) {
    this.auditLogRepository = auditLogRepository;
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
    this.entityManager = entityManager;
    this.auditFailureRecorder = auditFailureRecorder;
  }

  @Around("@annotation(audited)")
  public Object logAudit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
    Object[] args = joinPoint.getArgs();
    Object result;
    try {
      result = joinPoint.proceed();
    } catch (Throwable ex) {
      recordAttempt(audited, args, ex);
      throw ex;
    }

    try {
      // AuditLog ke thua TenantScopedEntity (tenant_id NOT NULL) — lay thang tu TenantContext
      // (JwtAuthenticationFilter da ghi san, khong can query lai user) thay vi tu User, tranh loi
      // vi pham NOT NULL neu vi ly do nao do khong tim thay User (vd tai khoan vua bi vo hieu hoa
      // giua luc request dang chay).
      Long tenantId = TenantContext.get();
      if (tenantId == null) {
        log.warn(
            "Bo qua ghi audit log cho action={}: khong xac dinh duoc tenant hien tai",
            audited.action());
        return result;
      }
      AuditLog auditLog = new AuditLog();
      auditLog.setTenant(entityManager.getReference(Tenant.class, tenantId));
      auditLog.setAction(audited.action());
      auditLog.setEntityName(audited.entityName().isBlank() ? null : audited.entityName());
      auditLog.setBefore(safeWriteValue(args.length > 0 ? args[0] : null));
      auditLog.setAfter(safeWriteValue(result));
      currentUsername()
          .flatMap(userRepository::findByUsernameAndActiveTrue)
          .ifPresent(auditLog::setUser);
      auditLogRepository.save(auditLog);
      // Thao tac xoa la su kien nhay cam (Prompt #8, P2 quan sat) - ngoai dong da ghi vao bang
      // audit_logs (chi tra van duoc bang truy van DB), them 1 dong log WARN de he thong log tap
      // trung (ELK/Loki...) bat duoc ngay ma khong can join sang DB - ap dung CHUNG cho MOI action
      // @Audited co chua "DELETE" (vd PRODUCT_DELETE), khong can sua tung Service rieng le.
      if (audited.action().contains("DELETE")) {
        log.warn(
            "SENSITIVE_DELETE action={} entityName={} tenantId={} user={}",
            audited.action(),
            audited.entityName(),
            tenantId,
            currentUsername().orElse(null));
      }
    } catch (Exception ex) {
      // Ghi audit that bai khong duoc lam hong nghiep vu chinh — chi log canh bao.
      log.warn("Khong the ghi audit log cho action={}: {}", audited.action(), ex.getMessage());
    }

    return result;
  }

  /**
   * Hanh dong bi chan/that bai giua chung (vd 1 guard nghiep vu nem exception) truoc day hoan toan
   * khong de lai dau vet vi logAudit() chi ghi SAU khi proceed() thanh cong - mot hanh dong pha
   * hoai bi chan van nen biet duoc da co ai THU thuc hien (phat hien khi rieng soat).
   */
  private void recordAttempt(Audited audited, Object[] args, Throwable ex) {
    try {
      Long tenantId = TenantContext.get();
      if (tenantId == null) {
        return;
      }
      auditFailureRecorder.record(
          tenantId,
          audited.action() + "_ATTEMPT",
          audited.entityName().isBlank() ? null : audited.entityName(),
          safeWriteValue(args.length > 0 ? args[0] : null),
          safeWriteValue("that bai: " + ex.getMessage()),
          currentUsername().orElse(null));
    } catch (Exception loggingEx) {
      log.warn(
          "Khong the ghi audit log ATTEMPT cho action={}: {}",
          audited.action(),
          loggingEx.getMessage());
    }
  }

  private java.util.Optional<String> currentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(authentication.getName());
  }

  private String safeWriteValue(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "\"<khong the serialize: " + ex.getMessage() + ">\"";
    }
  }
}
