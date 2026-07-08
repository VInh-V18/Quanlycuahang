package com.quanlycuahang.erp.system.repository;

import com.quanlycuahang.erp.system.entity.Settings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, Long> {

  Optional<Settings> findByBranchIdAndKey(Long branchId, String key);

  Optional<Settings> findByBranchIdIsNullAndKey(String key);
}
