package com.quanlycuahang.erp.ai.repository;

import com.quanlycuahang.erp.inventory.entity.Inventory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Truy van tho phuc vu goi y nhap hang (Prompt #11, tinh nang dot 1b) - KHONG dua vao pool AI rieng
 * (V30/AiReadOnlyDataSourceConfig) vi day la thuat toan xac dinh (deterministic, khong goi LLM -
 * xem Javadoc AiPurchaseSuggestionService) chay qua DataSource CHINH cua ung dung nhu moi
 * repository khac, khong phai luong AI thuc su doc du lieu.
 *
 * <p>Ke thua {@code JpaRepository<Inventory, Long>} (thay vi marker {@code Repository<Object,
 * Long>} - da thu va THAT BAI luc chay: Spring Data JPA khong nhan dien duoc {@code Object} nhu 1
 * kieu entity hop le nen KHONG tao bean cho repository, "No qualifying bean... available") - dung
 * dung khuon mau da CHUNG MINH hoat dong cua {@code InventoryRepository.findInventoryValueByBranch}
 * (native query tra ve {@code Object[]} voi cot HOAN TOAN khac Inventory) - kieu entity generic chi
 * anh huong cac ham CRUD ke thua, khong bat buoc {@code @Query} tuy chinh phai tra ve dung kieu do.
 *
 * <p>Native query (khong qua Hibernate @Filter) nen PHAI tu truyen tenantId tuong minh, giong 12
 * file native-query khac trong he thong (xem TenantScopedEntity Javadoc).
 */
public interface AiPurchaseSuggestionRepository extends JpaRepository<Inventory, Long> {

  @Query(
      value =
          "SELECT p.id AS product_id, p.name AS product_name, p.sku AS sku, "
              + "i.stock AS current_stock, p.min_stock AS min_stock, "
              + "COALESCE(sold.qty_30d, 0) AS qty_sold_30d "
              + "FROM inventory i "
              + "JOIN products p ON p.id = i.product_id "
              + "LEFT JOIN ("
              + "  SELECT oi.product_id AS product_id, SUM(oi.quantity) AS qty_30d "
              + "  FROM order_items oi "
              + "  JOIN orders o ON o.id = oi.order_id "
              + "  WHERE o.branch_id = :branchId AND o.tenant_id = :tenantId "
              + "    AND o.created_at >= now() - interval '30 days' "
              + "    AND o.status IN ('completed', 'partially_returned', 'fully_returned') "
              + "    AND o.deleted_at IS NULL AND oi.deleted_at IS NULL "
              + "  GROUP BY oi.product_id"
              + ") sold ON sold.product_id = p.id "
              + "WHERE i.branch_id = :branchId AND i.tenant_id = :tenantId AND p.deleted_at IS NULL",
      nativeQuery = true)
  List<Object[]> findVelocityDataForBranch(
      @Param("branchId") Long branchId, @Param("tenantId") Long tenantId);
}
