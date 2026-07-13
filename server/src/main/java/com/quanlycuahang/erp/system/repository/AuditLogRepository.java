package com.quanlycuahang.erp.system.repository;

import com.quanlycuahang.erp.system.entity.AuditLog;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  /**
   * Lich su audit co loc, dung cho trang Nhat ky audit (chi Owner/Quan ly xem duoc) — cung tieu chi
   * loc voi cac danh sach khac (khoang ngay + tim theo hanh dong/doi tuong).
   */
  @Query(
      value =
          "SELECT a.id, a.created_at, u.full_name, a.action, a.entity_name, a.entity_id, "
              + "CAST(a.before AS text), CAST(a.after AS text) "
              + "FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id "
              + "WHERE a.deleted_at IS NULL AND a.tenant_id = :tenantId "
              + "AND (CAST(:from AS timestamptz) IS NULL OR a.created_at >= CAST(:from AS timestamptz)) "
              + "AND (CAST(:to AS timestamptz) IS NULL OR a.created_at < CAST(:to AS timestamptz)) "
              + "AND (:search = '' OR a.action ILIKE '%' || :search || '%' "
              + "     OR a.entity_name ILIKE '%' || :search || '%' "
              + "     OR u.full_name ILIKE '%' || :search || '%') "
              + "ORDER BY a.created_at DESC",
      countQuery =
          "SELECT count(*) FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id "
              + "WHERE a.deleted_at IS NULL AND a.tenant_id = :tenantId "
              + "AND (CAST(:from AS timestamptz) IS NULL OR a.created_at >= CAST(:from AS timestamptz)) "
              + "AND (CAST(:to AS timestamptz) IS NULL OR a.created_at < CAST(:to AS timestamptz)) "
              + "AND (:search = '' OR a.action ILIKE '%' || :search || '%' "
              + "     OR a.entity_name ILIKE '%' || :search || '%' "
              + "     OR u.full_name ILIKE '%' || :search || '%')",
      nativeQuery = true)
  Page<Object[]> search(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("search") String search,
      @Param("tenantId") Long tenantId,
      Pageable pageable);
}
