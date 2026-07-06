package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  Optional<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);

  /** Khoa pessimistic khi can tranh race condition luc nhap/ban dong thoi cung 1 dong ton kho. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.branch.id = :branchId")
  Optional<Inventory> findByProductIdAndBranchIdForUpdate(
      @Param("productId") Long productId, @Param("branchId") Long branchId);

  Page<Inventory> findByBranchId(Long branchId, Pageable pageable);

  java.util.List<Inventory> findByBranchId(Long branchId);

  List<Inventory> findByBranchIdAndProductIdIn(Long branchId, List<Long> productIds);

  @Query(
      "SELECT i FROM Inventory i WHERE i.branch.id = :branchId AND i.stock <= i.product.minStock")
  Page<Inventory> findLowStockByBranchId(@Param("branchId") Long branchId, Pageable pageable);

  /**
   * Gia tri ton kho theo gia von (Phase 10) — nhom theo chi nhanh (:branchId = null xem tat ca).
   */
  @Query(
      value =
          "SELECT b.name AS label, COALESCE(SUM(i.stock * i.cost_price), 0) AS total_value, "
              + "COALESCE(SUM(i.stock), 0) AS total_quantity "
              + "FROM inventory i JOIN branches b ON b.id = i.branch_id "
              + "WHERE (:branchId IS NULL OR i.branch_id = :branchId) "
              + "GROUP BY b.id, b.name ORDER BY total_value DESC",
      nativeQuery = true)
  List<Object[]> findInventoryValueByBranch(@Param("branchId") Long branchId);

  /** Gia tri ton kho theo gia von, nhom theo danh muc san pham trong 1 chi nhanh (Phase 10). */
  @Query(
      value =
          "SELECT c.name AS label, COALESCE(SUM(i.stock * i.cost_price), 0) AS total_value, "
              + "COALESCE(SUM(i.stock), 0) AS total_quantity "
              + "FROM inventory i "
              + "JOIN products p ON p.id = i.product_id "
              + "JOIN categories c ON c.id = p.category_id "
              + "WHERE (:branchId IS NULL OR i.branch_id = :branchId) "
              + "GROUP BY c.id, c.name ORDER BY total_value DESC",
      nativeQuery = true)
  List<Object[]> findInventoryValueByCategory(@Param("branchId") Long branchId);
}
