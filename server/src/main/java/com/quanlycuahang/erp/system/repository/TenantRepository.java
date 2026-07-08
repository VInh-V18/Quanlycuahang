package com.quanlycuahang.erp.system.repository;

import com.quanlycuahang.erp.system.entity.Tenant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

  List<Tenant> findAllByOrderByIdAsc();
}
