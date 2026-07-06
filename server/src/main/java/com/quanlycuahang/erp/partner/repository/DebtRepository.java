package com.quanlycuahang.erp.partner.repository;

import com.quanlycuahang.erp.partner.entity.Debt;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DebtRepository extends JpaRepository<Debt, Long> {

  Page<Debt> findByCustomerId(Long customerId, Pageable pageable);

  Page<Debt> findBySupplierId(Long supplierId, Pageable pageable);

  java.util.List<Debt> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

  /** Tong cong no con du (chua co co che tru dan qua DebtPayment — xem PROJECT_STATE FH-12). */
  @Query(
      "SELECT COALESCE(SUM(d.amount), 0) FROM Debt d "
          + "WHERE d.supplier.id = :supplierId AND d.direction = 'payable' AND d.amount > 0")
  BigDecimal sumOutstandingBySupplierId(@Param("supplierId") Long supplierId);

  /**
   * Tuoi no tinh tu ngay tao Debt (created_at) den hien tai, chia 4 muc chuan (0-30/31-60/61-90/
   * >90 ngay) — chi tinh cong no con du (amount > 0), theo dung chieu receivable (KH no) hoac
   * payable (phai tra NCC) (Phase 10).
   */
  @Query(
      value =
          "SELECT CASE "
              + "  WHEN EXTRACT(DAY FROM now() - d.created_at) <= 30 THEN '0-30' "
              + "  WHEN EXTRACT(DAY FROM now() - d.created_at) <= 60 THEN '31-60' "
              + "  WHEN EXTRACT(DAY FROM now() - d.created_at) <= 90 THEN '61-90' "
              + "  ELSE '90+' END AS bucket, "
              + "COALESCE(SUM(d.amount), 0) AS total_amount, COUNT(*) AS debt_count "
              + "FROM debts d "
              + "WHERE d.direction = :direction AND d.amount > 0 "
              + "GROUP BY 1 "
              + "ORDER BY MIN(EXTRACT(DAY FROM now() - d.created_at))",
      nativeQuery = true)
  List<Object[]> findAgingBuckets(@Param("direction") String direction);
}
