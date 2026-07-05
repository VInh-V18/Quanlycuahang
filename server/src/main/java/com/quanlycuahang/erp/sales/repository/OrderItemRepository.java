package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  List<OrderItem> findByOrderId(Long orderId);
}
