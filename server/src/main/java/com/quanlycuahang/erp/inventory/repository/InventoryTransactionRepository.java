package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** The kho (D4: index composite product_id + created_at cho query nay). */
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

  Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

  /** The kho loc theo chi nhanh (tranh lo so luong/gia von chi nhanh khac qua endpoint nay). */
  Page<InventoryTransaction> findByProductIdAndBranchIdOrderByCreatedAtDesc(
      Long productId, Long branchId, Pageable pageable);

  Optional<InventoryTransaction> findByPurchaseOrderItemId(Long purchaseOrderItemId);

  /**
   * Toan bo lich su bien dong ton (moi loai) cua 1 san pham/chi nhanh, theo dung thu tu thoi gian -
   * dung de "phat lai" (replay) va tinh lai gia von khi sua gia 1 phieu nhap cu.
   */
  @Query(
      "SELECT t FROM InventoryTransaction t "
          + "WHERE t.product.id = :productId AND t.branch.id = :branchId "
          + "ORDER BY t.createdAt ASC, t.id ASC")
  List<InventoryTransaction> findAllForCostReplay(
      @Param("productId") Long productId, @Param("branchId") Long branchId);
}
