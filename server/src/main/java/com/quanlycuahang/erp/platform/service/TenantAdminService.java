package com.quanlycuahang.erp.platform.service;

import com.quanlycuahang.erp.auth.entity.Role;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.RoleRepository;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.platform.dto.TenantCreateRequest;
import com.quanlycuahang.erp.platform.dto.TenantResponse;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import com.quanlycuahang.erp.system.repository.TenantRepository;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin tao/xem/khoa Tenant (cua hang) - CHI Super Admin duoc goi (khong co dang ky cong
 * khai). Tao tenant moi luon kem 1 chi nhanh mac dinh + 1 tai khoan chu cua hang (role "owner", da
 * co san toan quyen trong tenant do qua bang role_permissions dung chung moi tenant) de chu cua
 * hang tu dang nhap va tu cau hinh moi thu con lai, khong can Super Admin lam thay.
 *
 * <p>Khong dua vao TenantContext o day: Branch/User moi tao duoc gan tenant qua setTenant(...)
 * TRUC TIEP (khong phai qua @PrePersist doc TenantContext) vi luong Super Admin khong dang nhap
 * vao 1 tenant nao ca - TenantContext luon null suot request nay, dung y nhu vay (moi truy van doc
 * o Service nay tu nhien khong bi Hibernate @Filter gioi han, dung dang cho "xem duoc moi tenant").
 */
@Service
public class TenantAdminService {

  private static final String DEFAULT_BRANCH_NAME = "Chi nhanh 1";
  private static final String OWNER_ROLE_CODE = "owner";

  private final TenantRepository tenantRepository;
  private final BranchRepository branchRepository;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public TenantAdminService(
      TenantRepository tenantRepository,
      BranchRepository branchRepository,
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    this.tenantRepository = tenantRepository;
    this.branchRepository = branchRepository;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<TenantResponse> listTenants() {
    return tenantRepository.findAllByOrderByIdAsc().stream()
        .map(TenantAdminService::toResponse)
        .toList();
  }

  @Transactional
  public TenantResponse createTenant(TenantCreateRequest request) {
    if (userRepository.existsByUsername(request.getOwnerUsername())) {
      throw new BusinessRuleException(
          "TENANT_OWNER_USERNAME_TAKEN", "Ten dang nhap nay da duoc su dung");
    }
    Role ownerRole =
        roleRepository
            .findByCode(OWNER_ROLE_CODE)
            .orElseThrow(
                () -> new IllegalStateException("Thieu du lieu he thong: role '" + OWNER_ROLE_CODE + "'"));

    Tenant tenant = new Tenant();
    tenant.setName(request.getTenantName());
    tenant.setActive(true);
    tenant = tenantRepository.save(tenant);

    Branch branch = new Branch();
    branch.setTenant(tenant);
    branch.setName(blankToDefault(request.getBranchName(), DEFAULT_BRANCH_NAME));
    branch.setActive(true);
    branch = branchRepository.save(branch);

    User owner = new User();
    owner.setTenant(tenant);
    owner.setUsername(request.getOwnerUsername());
    owner.setPasswordHash(passwordEncoder.encode(request.getOwnerPassword()));
    owner.setFullName(request.getOwnerFullName());
    owner.setActive(true);
    owner.setRoles(Set.of(ownerRole));
    owner.setBranches(Set.of(branch));
    userRepository.save(owner);

    return toResponse(tenant);
  }

  @Transactional
  public TenantResponse setActive(Long tenantId, boolean active) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tenant"));
    tenant.setActive(active);
    return toResponse(tenantRepository.save(tenant));
  }

  private static String blankToDefault(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }

  private static TenantResponse toResponse(Tenant tenant) {
    return new TenantResponse(
        tenant.getId(), tenant.getName(), tenant.isActive(), tenant.getCreatedAt());
  }
}
