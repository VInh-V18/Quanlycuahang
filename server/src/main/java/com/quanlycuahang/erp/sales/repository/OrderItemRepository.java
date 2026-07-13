package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.OrderItem;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  List<OrderItem> findByOrderId(Long orderId);

  /**
   * Khoa pessimistic khi doc/sua returnedQuantity trong ReturnService.createReturn() - tranh 2 yeu
   * cau tra hang dong thoi tren cung 1 dong don doc cung gia tri cu roi cung ghi de (double-refund
   * + cong kho 2 lan, phat hien khi audit).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :id")
  Optional<OrderItem> findByIdForUpdate(@Param("id") Long id);

  @Query(
      value =
          "SELECT p.id, p.name, p.sku, COALESCE(SUM(oi.quantity), 0) AS quantity_sold, "
              + "COALESCE(SUM(oi.line_total), 0) AS revenue "
              + "FROM order_items oi "
              + "JOIN orders o ON o.id = oi.order_id "
              + "JOIN products p ON p.id = oi.product_id "
              + "WHERE o.status IN "
              + OrderRepository.REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId) "
              + "GROUP BY p.id, p.name, p.sku ORDER BY quantity_sold DESC LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findTopProducts(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("limit") int limit,
      @Param("tenantId") Long tenantId);

  /**
   * COGS = Sigma(costPriceSnapshot x quantity ban goc, KHONG tru returnedQuantity) — dung dung cong
   * thuc B4 loi nhuan gop, phan hoan tra tinh rieng o buoc khac (Phase 10).
   */
  @Query(
      value =
          "SELECT COALESCE(SUM(oi.cost_price_snapshot * oi.quantity), 0) "
              + "FROM order_items oi "
              + "JOIN orders o ON o.id = oi.order_id "
              + "WHERE o.status IN "
              + OrderRepository.REVENUE_STATUSES
              + " "
              + "AND o.created_at >= :from AND o.created_at < :to AND o.tenant_id = :tenantId "
              + "AND (:branchId IS NULL OR o.branch_id = :branchId)",
      nativeQuery = true)
  BigDecimal sumCostOfGoodsSold(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("branchId") Long branchId,
      @Param("tenantId") Long tenantId);
}
