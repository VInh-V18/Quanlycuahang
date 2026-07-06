package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.Return;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRepository extends JpaRepository<Return, Long> {

  /**
   * Tong tien hoan tra phat sinh TRONG ky bao cao (theo ngay tao phieu tra, khong phai ngay ban
   * goc) — dung lam "anh huong hoan tra" trong cong thuc loi nhuan gop B4 (Phase 10).
   */
  @Query(
      value =
          "SELECT COALESCE(SUM(r.total_refund), 0) "
              + "FROM returns r JOIN orders o ON o.id = r.order_id "
              + "WHERE r.created_at >= :from AND r.created_at < :to "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId)",
      nativeQuery = true)
  BigDecimal sumReturnImpact(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId);

  /** Số đơn trả hàng + tổng tiền hoàn trong kỳ, dùng cho thẻ KPI "Khách trả hàng" ở Dashboard. */
  @Query(
      value =
          "SELECT COUNT(*), COALESCE(SUM(r.total_refund), 0) "
              + "FROM returns r JOIN orders o ON o.id = r.order_id "
              + "WHERE r.created_at >= :from AND r.created_at < :to "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId)",
      nativeQuery = true)
  List<Object[]> countAndSumReturns(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId);
}
