package com.quanlycuahang.erp.operation.repository;

import com.quanlycuahang.erp.operation.entity.CashTransaction;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

  List<CashTransaction> findByShiftIdOrderByCreatedAtDesc(Long shiftId);

  @Query(
      "SELECT COALESCE(SUM(ct.amount), 0) FROM CashTransaction ct "
          + "WHERE ct.shift.id = :shiftId AND ct.type = :type")
  BigDecimal sumByShiftIdAndType(@Param("shiftId") Long shiftId, @Param("type") String type);
}
