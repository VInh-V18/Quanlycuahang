package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.OrderPayment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

  List<OrderPayment> findByOrderId(Long orderId);
}
