package com.quanlycuahang.erp.system.repository;

import com.quanlycuahang.erp.system.entity.Tenant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

  List<Tenant> findAllByOrderByIdAsc();

  /**
   * Dung cho ReconciliationScheduledJob (Prompt #6) - chi chay doi soat cho tenant dang hoat dong,
   * bo qua tenant da bi Super Admin khoa.
   */
  List<Tenant> findByActiveTrueOrderByIdAsc();
}
