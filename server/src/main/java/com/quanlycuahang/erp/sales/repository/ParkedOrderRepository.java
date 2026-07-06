package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.ParkedOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkedOrderRepository extends JpaRepository<ParkedOrder, Long> {

  List<ParkedOrder> findByBranchIdOrderByParkedAtDesc(Long branchId);
}
