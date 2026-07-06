package com.quanlycuahang.erp.sales.repository;

import com.quanlycuahang.erp.sales.entity.OrderPayment;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

  List<OrderPayment> findByOrderId(Long orderId);

  /**
   * Tong tien theo hinh thuc thanh toan cua cac don ban trong 1 ca — dung cho doi chieu ket tien
   * khi dong ca (FH-14).
   */
  @Query(
      "SELECT COALESCE(SUM(op.amount), 0) FROM OrderPayment op "
          + "WHERE op.order.shift.id = :shiftId AND op.method = :method")
  BigDecimal sumByShiftIdAndMethod(@Param("shiftId") Long shiftId, @Param("method") String method);
}
