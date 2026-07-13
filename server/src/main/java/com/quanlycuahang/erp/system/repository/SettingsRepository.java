package com.quanlycuahang.erp.system.repository;

import com.quanlycuahang.erp.system.entity.Settings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, Long> {

  Optional<Settings> findByBranchIdAndKey(Long branchId, String key);

  Optional<Settings> findByBranchIdIsNullAndKey(String key);

  /**
   * Dung khi CHUA co TenantContext (vd doc gioi han dang nhap truoc khi xac thuc trong
   * AuthService.login) - @Filter Hibernate khong bat trong tinh huong nay nen phai loc tenant_id
   * TUONG MINH, khong dua vao filter tu dong.
   */
  Optional<Settings> findByTenantIdAndBranchIdIsNullAndKey(Long tenantId, String key);
}
