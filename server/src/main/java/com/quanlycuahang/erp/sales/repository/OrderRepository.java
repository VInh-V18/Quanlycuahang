package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.Order;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository
    extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

  /** Trang thai tinh la "da ban" (loai draft/cancelled) dung chung cho moi bao cao doanh thu. */
  String REVENUE_STATUSES = "('completed','partially_returned','fully_returned')";

  Optional<Order> findByOrderNumber(String orderNumber);

  /**
   * Khoa pessimistic khi sua don da hoan tat (OrderEditService.editOrder) - tranh Sua don/Tra
   * hang/Huy don chay dong thoi tren cung 1 don doc cung trang thai cu roi cung ghi de (mirror
   * OrderItemRepository.findByIdForUpdate).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.id = :id")
  Optional<Order> findByIdForUpdate(@Param("id") Long id);

  long countByShiftId(Long shiftId);

  /**
   * Doanh thu nhom theo ngay/tuan/thang (:unit = 'day'|'week'|'month') — quy ve gio dia phuong
   * Asia/Ho_Chi_Minh truoc khi cat, tranh lech ngay so voi UTC luu trong DB (Phase 10). Kem gia von
   * (cost_of_goods_sold) tinh rieng qua subquery tren order_items roi LEFT JOIN theo label, tranh
   * nhan doi revenue neu JOIN truc tiep order_items (1 don co nhieu dong) — dung ve doi thi ban FE
   * thanh chart 2 chuoi Doanh thu/Loi nhuan gop (FH-15).
   */
  @Query(
      value =
          "SELECT r.label, r.revenue, r.order_count, COALESCE(c.cost_of_goods_sold, 0) "
              + "FROM ("
              + "  SELECT to_char(date_trunc(:unit, o.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh'), 'YYYY-MM-DD') AS label, "
              + "  COALESCE(SUM(o.total_amount), 0) AS revenue, COUNT(*) AS order_count "
              + "  FROM orders o WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "  AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId "
              + "  AND (:branchId IS NULL OR o.branch_id = :branchId) GROUP BY 1"
              + ") r LEFT JOIN ("
              + "  SELECT to_char(date_trunc(:unit, o.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh'), 'YYYY-MM-DD') AS label, "
              + "  COALESCE(SUM(oi.cost_price_snapshot * oi.quantity), 0) AS cost_of_goods_sold "
              + "  FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "  AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId "
              + "  AND (:branchId IS NULL OR o.branch_id = :branchId) GROUP BY 1"
              + ") c ON c.label = r.label "
              + "ORDER BY r.label",
      nativeQuery = true)
  List<Object[]> findRevenueByPeriod(
      @Param("unit") String unit,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("tenantId") Long tenantId);

  @Query(
      value =
          "SELECT r.label, r.revenue, r.order_count, COALESCE(c.cost_of_goods_sold, 0) "
              + "FROM ("
              + "  SELECT b.id AS branch_id, b.name AS label, COALESCE(SUM(o.total_amount), 0) AS revenue, COUNT(*) AS order_count "
              + "  FROM orders o JOIN branches b ON b.id = o.branch_id WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "  AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId GROUP BY b.id, b.name"
              + ") r LEFT JOIN ("
              + "  SELECT o.branch_id, COALESCE(SUM(oi.cost_price_snapshot * oi.quantity), 0) AS cost_of_goods_sold "
              + "  FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "  AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId GROUP BY o.branch_id"
              + ") c ON c.branch_id = r.branch_id "
              + "ORDER BY r.revenue DESC",
      nativeQuery = true)
  List<Object[]> findRevenueByBranch(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("tenantId") Long tenantId);

  @Query(
      value =
          "SELECT r.label, r.revenue, r.order_count, COALESCE(c.cost_of_goods_sold, 0) "
              + "FROM ("
              + "  SELECT u.id AS cashier_id, u.full_name AS label, COALESCE(SUM(o.total_amount), 0) AS revenue, COUNT(*) AS order_count "
              + "  FROM orders o JOIN users u ON u.id = o.cashier_id WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "  AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId "
              + "  AND (:branchId IS NULL OR o.branch_id = :branchId) GROUP BY u.id, u.full_name"
              + ") r LEFT JOIN ("
              + "  SELECT o.cashier_id, COALESCE(SUM(oi.cost_price_snapshot * oi.quantity), 0) AS cost_of_goods_sold "
              + "  FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "  AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId "
              + "  AND (:branchId IS NULL OR o.branch_id = :branchId) GROUP BY o.cashier_id"
              + ") c ON c.cashier_id = r.cashier_id "
              + "ORDER BY r.revenue DESC",
      nativeQuery = true)
  List<Object[]> findRevenueByCashier(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("tenantId") Long tenantId);

  @Query(
      value =
          "SELECT COALESCE(SUM(o.total_amount), 0) "
              + "FROM orders o "
              + "WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId)",
      nativeQuery = true)
  BigDecimal sumRevenue(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("tenantId") Long tenantId);

  @Query(
      value =
          "SELECT c.id, c.name, COUNT(*) AS order_count, COALESCE(SUM(o.total_amount), 0) AS revenue "
              + "FROM orders o JOIN customers c ON c.id = o.customer_id "
              + "WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId) "
              + "GROUP BY c.id, c.name ORDER BY revenue DESC LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findTopCustomers(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("limit") int limit,
      @Param("tenantId") Long tenantId);

  /**
   * Danh sach don gan day nhat cho Dashboard — co danh dau has_debt (EXISTS truc tiep tren
   * debts.reference_type='order') vi trang thai "Ghi nợ" khong phai 1 gia tri cua orders.status
   * (state machine chi co draft/completed/partially_returned/fully_returned/cancelled) ma la cong
   * no con treo phat sinh khi ban chua thu du tien (OrderService.createOrder).
   */
  @Query(
      value =
          "SELECT o.order_number, COALESCE(c.name, 'Khách lẻ') AS customer_name, o.total_amount, o.status, "
              + "EXISTS(SELECT 1 FROM debts d WHERE d.reference_type = 'order' AND d.reference_id = o.id "
              + "AND d.direction = 'receivable' AND d.deleted_at IS NULL) AS has_debt "
              + "FROM orders o LEFT JOIN customers c ON c.id = o.customer_id "
              + "WHERE o.tenant_id = :tenantId AND (:branchId IS NULL OR o.branch_id = :branchId) "
              + "ORDER BY o.created_at DESC LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findRecentOrders(
      @Param("branchId") Long branchId,
      @Param("limit") int limit,
      @Param("tenantId") Long tenantId);

  /**
   * Danh sach don hang co loc, dung cho trang Don hang (FH-9). payment_methods gop cac phuong thuc
   * thanh toan da ghi nhan (co the rong neu don ghi no hoan toan chua thu dong nao).
   */
  @Query(
      value =
          "SELECT o.id, o.order_number, o.created_at, o.status, o.total_amount, "
              + "COALESCE(c.name, 'Khách lẻ') AS customer_name, c.phone AS customer_phone, "
              + "u.full_name AS cashier_name, "
              + "EXISTS(SELECT 1 FROM debts d WHERE d.reference_type = 'order' AND d.reference_id = o.id "
              + "AND d.direction = 'receivable' AND d.amount > 0 AND d.deleted_at IS NULL) AS has_debt, "
              + "(SELECT string_agg(DISTINCT p.method, ',') FROM order_payments p "
              + "WHERE p.order_id = o.id AND p.deleted_at IS NULL) AS payment_methods "
              + "FROM orders o LEFT JOIN customers c ON c.id = o.customer_id "
              + "JOIN users u ON u.id = o.cashier_id "
              + "WHERE o.branch_id = :branchId AND o.tenant_id = :tenantId "
              + "AND (CAST(:from AS timestamptz) IS NULL OR o.created_at >= CAST(:from AS timestamptz)) "
              + "AND (CAST(:to AS timestamptz) IS NULL OR o.created_at < CAST(:to AS timestamptz)) "
              + "AND (CAST(:status AS varchar) IS NULL OR o.status = CAST(:status AS varchar)) "
              + "AND (CAST(:cashierId AS bigint) IS NULL OR o.cashier_id = CAST(:cashierId AS bigint)) "
              + "AND (:search = '' OR o.order_number ILIKE '%' || :search || '%' "
              + "     OR c.name ILIKE '%' || :search || '%' OR c.phone ILIKE '%' || :search || '%') "
              + "ORDER BY o.created_at DESC",
      countQuery =
          "SELECT count(*) FROM orders o LEFT JOIN customers c ON c.id = o.customer_id "
              + "WHERE o.branch_id = :branchId AND o.tenant_id = :tenantId "
              + "AND (CAST(:from AS timestamptz) IS NULL OR o.created_at >= CAST(:from AS timestamptz)) "
              + "AND (CAST(:to AS timestamptz) IS NULL OR o.created_at < CAST(:to AS timestamptz)) "
              + "AND (CAST(:status AS varchar) IS NULL OR o.status = CAST(:status AS varchar)) "
              + "AND (CAST(:cashierId AS bigint) IS NULL OR o.cashier_id = CAST(:cashierId AS bigint)) "
              + "AND (:search = '' OR o.order_number ILIKE '%' || :search || '%' "
              + "     OR c.name ILIKE '%' || :search || '%' OR c.phone ILIKE '%' || :search || '%')",
      nativeQuery = true)
  Page<Object[]> search(
      @Param("branchId") Long branchId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("status") String status,
      @Param("cashierId") Long cashierId,
      @Param("search") String search,
      @Param("tenantId") Long tenantId,
      Pageable pageable);
}
