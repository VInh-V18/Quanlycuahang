package com.quanlycuahang.erp.partner.repository;

import com.quanlycuahang.erp.partner.entity.DebtPayment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

  List<DebtPayment> findByDebtIdIn(List<Long> debtIds);
}
