package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** The kho (D4: index composite product_id + created_at cho query nay). */
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

  Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

  /** The kho loc theo chi nhanh (tranh lo so luong/gia von chi nhanh khac qua endpoint nay). */
  Page<InventoryTransaction> findByProductIdAndBranchIdOrderByCreatedAtDesc(
      Long productId, Long branchId, Pageable pageable);
}
