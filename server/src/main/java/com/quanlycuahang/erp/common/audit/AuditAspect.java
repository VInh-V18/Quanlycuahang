package com.quanlycuahang.erp.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.system.entity.AuditLog;
import com.quanlycuahang.erp.system.repository.AuditLogRepository;
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
 * ket qua tra ve (after) duoi dang JSON vao audit_logs (D3). Chi ghi khi method thanh cong — that
 * bai thi khong ghi (khong co gi de audit).
 */
@Aspect
@Component
public class AuditAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

  private final AuditLogRepository auditLogRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  public AuditAspect(
      AuditLogRepository auditLogRepository,
      UserRepository userRepository,
      ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
  }

  @Around("@annotation(audited)")
  public Object logAudit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
    Object[] args = joinPoint.getArgs();
    Object result = joinPoint.proceed();

    try {
      AuditLog auditLog = new AuditLog();
      auditLog.setAction(audited.action());
      auditLog.setEntityName(audited.entityName().isBlank() ? null : audited.entityName());
      auditLog.setBefore(safeWriteValue(args.length > 0 ? args[0] : null));
      auditLog.setAfter(safeWriteValue(result));
      currentUsername()
          .flatMap(userRepository::findByUsernameAndActiveTrue)
          .ifPresent(auditLog::setUser);
      auditLogRepository.save(auditLog);
    } catch (Exception ex) {
      // Ghi audit that bai khong duoc lam hong nghiep vu chinh — chi log canh bao.
      log.warn("Khong the ghi audit log cho action={}: {}", audited.action(), ex.getMessage());
    }

    return result;
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
