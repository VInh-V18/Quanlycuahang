package com.quanlycuahang.erp.auth.repository;

import com.quanlycuahang.erp.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsernameAndActiveTrue(String username);

  boolean existsByUsername(String username);

  /**
   * Kiem tra trung username TREN TOAN HE THONG (bo qua @Filter tenant qua native query) - username
   * la duy nhat GLOBAL (uq_users_username, khong ghep tenant_id) vi dang nhap chi bang username, he
   * thong tu suy ra tenant, khong co buoc chon "cua hang nao". existsByUsername() o tren khi goi TU
   * 1 tenant User dang dang nhap (vd EmployeeService.create) chi thay dung tenant cua chinh no
   * (@Filter dang bat), nen truoc day 1 chu cua hang co the "duyet" 1 username thuc ra da thuoc VE
   * TENANT KHAC, roi nhan loi 409 chung chung tu rang buoc DB thay vi thong bao ro ngay tu dau
   * (phat hien khi rieng soat). Ham nay PHAI dung cho moi validate truoc khi tao User o cac luong
   * co @Filter dang bat; khong can dung o TenantAdminService/TenantUserAdminService (Super Admin)
   * vi TenantContext da null san, existsByUsername() o do da la global san.
   */
  @Query(
      value =
          "SELECT EXISTS(SELECT 1 FROM users WHERE username = :username AND deleted_at IS NULL)",
      nativeQuery = true)
  boolean existsByUsernameGlobal(@Param("username") String username);

  /**
   * JOIN FETCH roles (+ DISTINCT de gop dong lap do fetch collection) — EmployeeService.toResponse
   * doc user.getRoles() cho tung nhan vien; danh sach nay thuong nho (vai chuc nhan vien/tenant)
   * nen N+1 o day khong nghiem trong bang Inventory/StockTakeItem/PurchaseOrderItem, nhung fix re
   * va nhat quan nen ap dung luon (Prompt #7).
   */
  @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles ORDER BY u.fullName ASC")
  List<User> findAllByOrderByFullNameAsc();

  /**
   * Dung boi Super Admin (platform/service/TenantUserAdminService) - TenantContext luon null luc do
   * (khong dang nhap vao tenant nao) nen can loc tenant_id tuong minh, khong dua vao @Filter.
   */
  @Query(
      "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.tenantId = :tenantId ORDER"
          + " BY u.fullName ASC")
  List<User> findByTenantIdOrderByFullNameAsc(@Param("tenantId") Long tenantId);
}
