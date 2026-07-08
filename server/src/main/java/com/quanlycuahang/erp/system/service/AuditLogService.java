package com.quanlycuahang.erp.system.service;

import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.system.dto.AuditLogResponse;
import com.quanlycuahang.erp.system.repository.AuditLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Doc lai nhat ky audit (ghi qua AuditAspect, Phase 6) — chi doc, khong sua/xoa duoc tu day. */
@Service
public class AuditLogService {

  private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<AuditLogResponse>> search(
      LocalDate from, LocalDate to, String search, Pageable pageable) {
    OffsetDateTime fromDateTime =
        from == null ? null : from.atStartOfDay(APP_ZONE).toOffsetDateTime();
    OffsetDateTime toDateTime =
        to == null ? null : to.plusDays(1).atStartOfDay(APP_ZONE).toOffsetDateTime();
    Page<Object[]> page =
        auditLogRepository.search(
            fromDateTime,
            toDateTime,
            search == null ? "" : search.trim(),
            TenantContext.get(),
            pageable);
    return ApiResponse.page(page.map(AuditLogService::toResponse));
  }

  private static AuditLogResponse toResponse(Object[] row) {
    AuditLogResponse response = new AuditLogResponse();
    response.setId(((Number) row[0]).longValue());
    response.setCreatedAt(toInstant(row[1]));
    response.setUserFullName((String) row[2]);
    response.setAction((String) row[3]);
    response.setEntityName((String) row[4]);
    response.setEntityId(row[5] == null ? null : ((Number) row[5]).longValue());
    response.setBefore((String) row[6]);
    response.setAfter((String) row[7]);
    return response;
  }

  private static Instant toInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof OffsetDateTime odt) {
      return odt.toInstant();
    }
    if (value instanceof java.sql.Timestamp ts) {
      return ts.toInstant();
    }
    throw new IllegalStateException("Khong the chuyen doi thoi gian: " + value.getClass());
  }
}
