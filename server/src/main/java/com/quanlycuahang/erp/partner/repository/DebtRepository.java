package com.quanlycuahang.erp.partner.repository;

import com.quanlycuahang.erp.partner.entity.Debt;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  /** Tong cong no con du cua 1 khach hang - dung de doi chieu voi Customer.debtLimit khi ban no. */
  @Query(
      "SELECT COALESCE(SUM(d.amount), 0) FROM Debt d "
          + "WHERE d.customer.id = :customerId AND d.direction = 'receivable' AND d.amount > 0")
  BigDecimal sumOutstandingByCustomerId(@Param("customerId") Long customerId);

  /**
   * Ban tong hop theo danh sach supplierId (gom nhieu truy van rieng le thanh 1) - dung cho
   * SupplierService.list() de tranh N+1 (truoc day goi sumOutstandingBySupplierId rieng cho tung
   * dong trong trang, phat hien khi rieng soat hieu nang).
   */
  @Query(
      "SELECT d.supplier.id, COALESCE(SUM(d.amount), 0) FROM Debt d "
          + "WHERE d.supplier.id IN :supplierIds AND d.direction = 'payable' AND d.amount > 0 "
          + "GROUP BY d.supplier.id")
  List<Object[]> sumOutstandingBySupplierIds(@Param("supplierIds") List<Long> supplierIds);

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
              + "WHERE d.direction = :direction AND d.amount > 0 AND d.deleted_at IS NULL "
              + "AND d.tenant_id = :tenantId "
              + "GROUP BY 1 "
              + "ORDER BY MIN(EXTRACT(DAY FROM now() - d.created_at))",
      nativeQuery = true)
  List<Object[]> findAgingBuckets(
      @Param("direction") String direction, @Param("tenantId") Long tenantId);

  /** Tong cong no con du + so doi tac dang no, dung cho 2 the KPI dau trang Cong no (FH-12). */
  @Query(
      "SELECT COALESCE(SUM(d.amount), 0), COUNT(DISTINCT COALESCE(d.customer.id, d.supplier.id)) "
          + "FROM Debt d WHERE d.direction = :direction AND d.amount > 0")
  List<Object[]> summaryByDirection(@Param("direction") String direction);

  /**
   * Tuoi no theo tung doi tac (khac voi findAgingBuckets() o tren la tong hop toan he thong, Phase
   * 10 report) — 3 muc dung mockup FruitHouse: 0-7/8-30/>30 ngay (FH-12).
   */
  @Query(
      value =
          "SELECT p.id AS partner_id, p.name AS partner_name, "
              + "COALESCE(SUM(d.amount), 0) AS total_debt, "
              + "COALESCE(SUM(CASE WHEN EXTRACT(DAY FROM now() - d.created_at) <= 7 THEN d.amount ELSE 0 END), 0) AS bucket_0_7, "
              + "COALESCE(SUM(CASE WHEN EXTRACT(DAY FROM now() - d.created_at) BETWEEN 8 AND 30 THEN d.amount ELSE 0 END), 0) AS bucket_8_30, "
              + "COALESCE(SUM(CASE WHEN EXTRACT(DAY FROM now() - d.created_at) > 30 THEN d.amount ELSE 0 END), 0) AS bucket_over_30 "
              + "FROM debts d "
              + "JOIN (SELECT id, name, 'customer' AS kind FROM customers "
              + "      UNION ALL SELECT id, name, 'supplier' AS kind FROM suppliers) p "
              + "ON p.id = COALESCE(d.customer_id, d.supplier_id) "
              + "AND p.kind = CASE WHEN :direction = 'receivable' THEN 'customer' ELSE 'supplier' END "
              + "WHERE d.direction = :direction AND d.amount > 0 AND d.deleted_at IS NULL "
              + "AND d.tenant_id = :tenantId "
              + "GROUP BY p.id, p.name "
              + "ORDER BY total_debt DESC",
      nativeQuery = true)
  List<Object[]> findAgingByPartner(
      @Param("direction") String direction, @Param("tenantId") Long tenantId);

  /**
   * Khoa pessimistic (SELECT ... FOR UPDATE) cac dong no con du se bi tru trong recordPayment - chi
   * dung 1 noi duy nhat (DebtService.recordPayment), khong co @Version tren Debt truoc do nen 2 lan
   * ghi nhan thanh toan dong thoi cho cung doi tac co the doc cung so du cu va mat cap nhat (phat
   * hien khi review bao mat/du lieu tai chinh toan he thong).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<Debt> findByCustomerIdAndDirectionAndAmountGreaterThanOrderByCreatedAtAsc(
      Long customerId, String direction, java.math.BigDecimal minAmount);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<Debt> findBySupplierIdAndDirectionAndAmountGreaterThanOrderByCreatedAtAsc(
      Long supplierId, String direction, java.math.BigDecimal minAmount);

  /**
   * Khoa pessimistic ban Debt gan voi 1 don hang/phieu nhap - dung trong
   * ReturnService.createReturn() va PurchaseOrderService.updateItemPrice() de tranh doc so du cu
   * khi DebtService.recordPayment() dang khoa/sua cung dong Debt do o luong khac (phat hien khi
   * rieng soat).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<Debt> findByReferenceTypeAndReferenceIdOrderByIdAsc(String referenceType, Long referenceId);

  /**
   * Lich su doi chieu 1 doi tac: hop nhat 2 nguon — luc phat sinh no (debts.original_amount, duong)
   * va luc thu/tra no (debt_payments.amount, am). KHONG bao gom truong hop tra hang lam giam no
   * truc tiep tren debts.amount (ReturnService) vi luc do KHONG ghi lai 1 dong lich su rieng — no
   * ton tai tu Phase 9/FH-9, ngoai pham vi FH-12 (chi xay Cong no chi tiet + Ghi nhan thanh toan,
   * chua sua lai ReturnService).
   */
  @Query(
      value =
          "SELECT * FROM ("
              + "SELECT d.created_at AS event_at, "
              + "CASE WHEN d.reference_type = 'order' THEN 'Bán nợ' "
              + "     WHEN d.reference_type = 'purchase_order' THEN 'Mua nợ' ELSE 'Phát sinh nợ' END AS label, "
              + "CASE WHEN d.reference_type = 'order' THEN 'HD' || lpad(d.reference_id::text, 6, '0') "
              + "     WHEN d.reference_type = 'purchase_order' THEN 'PN' || lpad(d.reference_id::text, 6, '0') "
              + "     ELSE NULL END AS reference_code, "
              + "d.original_amount AS amount "
              + "FROM debts d "
              + "WHERE d.direction = :direction AND d.deleted_at IS NULL AND d.tenant_id = :tenantId "
              + "AND (:customerId IS NULL OR d.customer_id = CAST(:customerId AS bigint)) "
              + "AND (:supplierId IS NULL OR d.supplier_id = CAST(:supplierId AS bigint)) "
              + "UNION ALL "
              + "SELECT dp.paid_at AS event_at, "
              + "CASE WHEN :direction = 'receivable' THEN 'Thu nợ ' ELSE 'Trả nợ ' END "
              + "|| CASE WHEN dp.method = 'cash' THEN 'tiền mặt' WHEN dp.method = 'bank_transfer' THEN 'chuyển khoản' ELSE COALESCE(dp.method, '') END AS label, "
              + "NULL AS reference_code, "
              + "-dp.amount AS amount "
              + "FROM debt_payments dp JOIN debts d ON d.id = dp.debt_id "
              + "WHERE d.direction = :direction AND dp.deleted_at IS NULL AND d.tenant_id = :tenantId "
              + "AND (:customerId IS NULL OR d.customer_id = CAST(:customerId AS bigint)) "
              + "AND (:supplierId IS NULL OR d.supplier_id = CAST(:supplierId AS bigint))"
              + ") x ORDER BY event_at DESC",
      nativeQuery = true)
  List<Object[]> findHistory(
      @Param("direction") String direction,
      @Param("customerId") Long customerId,
      @Param("supplierId") Long supplierId,
      @Param("tenantId") Long tenantId);
}
