package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.Order;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository
    extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

  /** Trang thai tinh la "da ban" (loai draft/cancelled) dung chung cho moi bao cao doanh thu. */
  String REVENUE_STATUSES = "('completed','partially_returned','fully_returned')";

  Optional<Order> findByOrderNumber(String orderNumber);

  Page<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);

  /**
   * Doanh thu nhom theo ngay/tuan/thang (:unit = 'day'|'week'|'month') — quy ve gio dia phuong
   * Asia/Ho_Chi_Minh truoc khi cat, tranh lech ngay so voi UTC luu trong DB (Phase 10).
   */
  @Query(
      value =
          "SELECT to_char(date_trunc(:unit, o.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh'), 'YYYY-MM-DD') AS label, "
              + "COALESCE(SUM(o.total_amount), 0) AS revenue, COUNT(*) AS order_count "
              + "FROM orders o "
              + "WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId) "
              + "GROUP BY 1 ORDER BY 1",
      nativeQuery = true)
  List<Object[]> findRevenueByPeriod(
      @Param("unit") String unit,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId);

  @Query(
      value =
          "SELECT b.name AS label, COALESCE(SUM(o.total_amount), 0) AS revenue, COUNT(*) AS order_count "
              + "FROM orders o JOIN branches b ON b.id = o.branch_id "
              + "WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to "
              + "GROUP BY b.id, b.name ORDER BY revenue DESC",
      nativeQuery = true)
  List<Object[]> findRevenueByBranch(
      @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

  @Query(
      value =
          "SELECT u.full_name AS label, COALESCE(SUM(o.total_amount), 0) AS revenue, COUNT(*) AS order_count "
              + "FROM orders o JOIN users u ON u.id = o.cashier_id "
              + "WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId) "
              + "GROUP BY u.id, u.full_name ORDER BY revenue DESC",
      nativeQuery = true)
  List<Object[]> findRevenueByCashier(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId);

  @Query(
      value =
          "SELECT COALESCE(SUM(o.total_amount), 0) "
              + "FROM orders o "
              + "WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId)",
      nativeQuery = true)
  BigDecimal sumRevenue(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId);

  @Query(
      value =
          "SELECT c.id, c.name, COUNT(*) AS order_count, COALESCE(SUM(o.total_amount), 0) AS revenue "
              + "FROM orders o JOIN customers c ON c.id = o.customer_id "
              + "WHERE o.status IN "
              + REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId) "
              + "GROUP BY c.id, c.name ORDER BY revenue DESC LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findTopCustomers(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("limit") int limit);

  /**
   * Danh sach don gan day nhat cho Dashboard — co danh dau has_debt (EXISTS truc tiep tren
   * debts.reference_type='order') vi trang thai "Ghi nợ" khong phai 1 gia tri cua orders.status
   * (state machine chi co draft/completed/partially_returned/fully_returned/cancelled) ma la
   * cong no con treo phat sinh khi ban chua thu du tien (OrderService.createOrder).
   */
  @Query(
      value =
          "SELECT o.order_number, COALESCE(c.name, 'Khách lẻ') AS customer_name, o.total_amount, o.status, "
              + "EXISTS(SELECT 1 FROM debts d WHERE d.reference_type = 'order' AND d.reference_id = o.id "
              + "AND d.direction = 'receivable' AND d.deleted_at IS NULL) AS has_debt "
              + "FROM orders o LEFT JOIN customers c ON c.id = o.customer_id "
              + "WHERE (:branchId IS NULL OR o.branch_id = :branchId) "
              + "ORDER BY o.created_at DESC LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findRecentOrders(@Param("branchId") Long branchId, @Param("limit") int limit);
}
