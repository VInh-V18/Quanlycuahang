package com.quanlycuahang.erp.auth.repository;

import com.quanlycuahang.erp.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsernameAndActiveTrue(String username);

  boolean existsByUsername(String username);

  List<User> findAllByOrderByFullNameAsc();

  /** Dung boi Super Admin (platform/service/TenantUserAdminService) - TenantContext luon null luc
   * do (khong dang nhap vao tenant nao) nen can loc tenant_id tuong minh, khong dua vao @Filter. */
  List<User> findByTenantIdOrderByFullNameAsc(Long tenantId);
}
