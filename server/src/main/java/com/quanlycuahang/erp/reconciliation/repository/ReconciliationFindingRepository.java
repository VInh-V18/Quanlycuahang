package com.quanlycuahang.erp.reconciliation.repository;

import com.quanlycuahang.erp.reconciliation.entity.ReconciliationFinding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationFindingRepository
    extends JpaRepository<ReconciliationFinding, Long> {

  /**
   * Dung cho canh bao tren DashboardPage - Hibernate @Filter da tu loc tenant, chi can dem (khong
   * nap tung dong) de nhanh du goi moi lan tai trang.
   */
  long countByStatus(String status);
}
