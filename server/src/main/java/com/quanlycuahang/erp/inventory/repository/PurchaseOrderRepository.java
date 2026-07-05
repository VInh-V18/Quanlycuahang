package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

  Page<PurchaseOrder> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);
}
