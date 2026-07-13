package com.quanlycuahang.erp.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.common.web.ClientIpResolver;
import com.quanlycuahang.erp.platform.entity.PlatformAuditLog;
import com.quanlycuahang.erp.platform.repository.PlatformAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Ghi nhat ky hanh dong Super Admin (xem PlatformAuditLog) - dung chung cho TenantAdminService va
 * TenantUserAdminService, cung mau voi AuditAspect (lay nguoi thuc hien qua SecurityContextHolder,
 * serialize detail qua ObjectMapper co san) nhung khong the dung lai AuditAspect vi no gan chat voi
 * AuditLog (bat buoc tenant_id).
 *
 * <p>Ghi kem IP/user-agent (qua RequestContextHolder thay vi inject HttpServletRequest de van hoat
 * dong dung khi goi tu thread nen/khong co request) - truoc day nhat ky chi co username, khong the
 * truy vet duoc ai THUC SU thuc hien 1 hanh dong neu tai khoan superadmin bi lo/chia se (phat hien
 * khi rieng soat, lien quan truc tiep 1 su co xoa tenant khong ro tac nhan).
 *
 * <p>Prompt #10 (P3, don dep no ky thuat): bo endpoint doc lai nhat ky (GET
 * /platform-admin/audit-logs, PlatformAuditLogController da xoa) - khong co trang FE nao goi toi,
 * khac voi cac man hinh platform-admin khac (tenants/roles/tenant-users) deu da co FE dung.
 * record() o duoi van giu nguyen - chi bo phan DOC LAI, khong anh huong viec GHI nhat ky.
 */
@Service
public class PlatformAuditService {

  private final PlatformAuditLogRepository platformAuditLogRepository;
  private final ObjectMapper objectMapper;

  public PlatformAuditService(
      PlatformAuditLogRepository platformAuditLogRepository, ObjectMapper objectMapper) {
    this.platformAuditLogRepository = platformAuditLogRepository;
    this.objectMapper = objectMapper;
  }

  public void record(String action, Long tenantId, String targetDescription, Object detail) {
    PlatformAuditLog log = new PlatformAuditLog();
    log.setPlatformAdminUsername(currentUsername());
    log.setAction(action);
    log.setTenantId(tenantId);
    log.setTargetDescription(targetDescription);
    log.setDetail(safeWriteValue(detail));
    HttpServletRequest request = currentRequest();
    if (request != null) {
      log.setClientIp(ClientIpResolver.resolve(request));
      log.setUserAgent(request.getHeader("User-Agent"));
    }
    platformAuditLogRepository.save(log);
  }

  private HttpServletRequest currentRequest() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
      return null;
    }
    return attrs.getRequest();
  }

  private String currentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null ? "unknown" : authentication.getName();
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
