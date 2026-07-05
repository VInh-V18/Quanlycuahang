package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository
    extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

  Optional<Order> findByOrderNumber(String orderNumber);

  Page<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);
}
