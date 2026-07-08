package com.quanlycuahang.erp.platform.repository;

import com.quanlycuahang.erp.platform.entity.PlatformAdmin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, Long> {

  Optional<PlatformAdmin> findByUsernameAndActiveTrue(String username);
}
