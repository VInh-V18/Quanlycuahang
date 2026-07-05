package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
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

  @Query(
      "SELECT i FROM Inventory i WHERE i.branch.id = :branchId AND i.stock <= i.product.minStock")
  Page<Inventory> findLowStockByBranchId(@Param("branchId") Long branchId, Pageable pageable);
}
