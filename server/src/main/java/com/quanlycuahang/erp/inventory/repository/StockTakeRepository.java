package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.StockTake;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTakeRepository extends JpaRepository<StockTake, Long> {

  Page<StockTake> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);
}
