package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.Return;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRepository extends JpaRepository<Return, Long> {

  /** Lich su phieu tra hang co loc, dung cho trang Tra hang (cung tieu chi loc voi Hoa don/Don
   * hang — FH-9) — kem so dong san pham da tra (item_count) qua subquery, khong JOIN thang vao
   * return_items de tranh nhan ban dong khi 1 phieu tra co nhieu dong. */
  @Query(
      value =
          "SELECT r.id, r.created_at, o.order_number, COALESCE(c.name, 'Khách lẻ') AS customer_name, "
              + "c.phone AS customer_phone, r.total_refund, r.refund_method, "
              + "(SELECT COUNT(*) FROM return_items ri WHERE ri.return_id = r.id AND ri.deleted_at IS NULL) AS item_count, "
              + "u.full_name AS created_by_name "
              + "FROM returns r JOIN orders o ON o.id = r.order_id "
              + "LEFT JOIN customers c ON c.id = o.customer_id "
              + "JOIN users u ON u.id = r.created_by "
              + "WHERE r.deleted_at IS NULL AND o.branch_id = :branchId AND r.tenant_id = :tenantId "
              + "AND (CAST(:from AS timestamptz) IS NULL OR r.created_at >= CAST(:from AS timestamptz)) "
              + "AND (CAST(:to AS timestamptz) IS NULL OR r.created_at < CAST(:to AS timestamptz)) "
              + "AND (:search = '' OR o.order_number ILIKE '%' || :search || '%' "
              + "     OR c.name ILIKE '%' || :search || '%' OR c.phone ILIKE '%' || :search || '%') "
              + "ORDER BY r.created_at DESC",
      countQuery =
          "SELECT count(*) FROM returns r JOIN orders o ON o.id = r.order_id "
              + "LEFT JOIN customers c ON c.id = o.customer_id "
              + "WHERE r.deleted_at IS NULL AND o.branch_id = :branchId AND r.tenant_id = :tenantId "
              + "AND (CAST(:from AS timestamptz) IS NULL OR r.created_at >= CAST(:from AS timestamptz)) "
              + "AND (CAST(:to AS timestamptz) IS NULL OR r.created_at < CAST(:to AS timestamptz)) "
              + "AND (:search = '' OR o.order_number ILIKE '%' || :search || '%' "
              + "     OR c.name ILIKE '%' || :search || '%' OR c.phone ILIKE '%' || :search || '%')",
      nativeQuery = true)
  Page<Object[]> search(
      @Param("branchId") Long branchId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("search") String search,
      @Param("tenantId") Long tenantId,
      Pageable pageable);

  /**
   * Tong tien hoan tra phat sinh TRONG ky bao cao (theo ngay tao phieu tra, khong phai ngay ban
   * goc) — dung lam "anh huong hoan tra" trong cong thuc loi nhuan gop B4 (Phase 10).
   */
  @Query(
      value =
          "SELECT COALESCE(SUM(r.total_refund), 0) "
              + "FROM returns r JOIN orders o ON o.id = r.order_id "
              + "WHERE r.created_at >= :from AND r.created_at < :to AND r.tenant_id = :tenantId "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId)",
      nativeQuery = true)
  BigDecimal sumReturnImpact(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("tenantId") Long tenantId);

  /** Số đơn trả hàng + tổng tiền hoàn trong kỳ, dùng cho thẻ KPI "Khách trả hàng" ở Dashboard. */
  @Query(
      value =
          "SELECT COUNT(*), COALESCE(SUM(r.total_refund), 0) "
              + "FROM returns r JOIN orders o ON o.id = r.order_id "
              + "WHERE r.created_at >= :from AND r.created_at < :to AND r.tenant_id = :tenantId "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId)",
      nativeQuery = true)
  List<Object[]> countAndSumReturns(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("tenantId") Long tenantId);

  /**
   * Tong tien hoan bang tien mat trong khoang thoi gian 1 ca lam viec — returns khong co shift_id
   * rieng (Phase 9) nen doi chieu theo cua so thoi gian [openedAt, closedAt-hoac-now) cua ca, dung
   * cho tinh "tien mat thuc te du kien" khi dong ca (FH-14).
   */
  @Query(
      value =
          "SELECT COALESCE(SUM(r.total_refund), 0) "
              + "FROM returns r JOIN orders o ON o.id = r.order_id "
              + "WHERE r.refund_method = 'cash' AND o.branch_id = :branchId AND r.tenant_id = :tenantId "
              + "AND r.created_at >= :from AND r.created_at < :to",
      nativeQuery = true)
  BigDecimal sumCashRefundsInWindow(
      @Param("branchId") Long branchId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("tenantId") Long tenantId);
}
