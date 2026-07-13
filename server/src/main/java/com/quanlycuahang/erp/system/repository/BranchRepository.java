package com.quanlycuahang.erp.system.repository;

import com.quanlycuahang.erp.system.entity.Branch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

  List<Branch> findByActiveTrueOrderByNameAsc();

  /**
   * Dung boi Super Admin (platform/service/TenantUserAdminService) - TenantContext luon null luc do
   * nen can loc tenant_id tuong minh, khong dua vao @Filter.
   */
  List<Branch> findByTenantId(Long tenantId);
}
