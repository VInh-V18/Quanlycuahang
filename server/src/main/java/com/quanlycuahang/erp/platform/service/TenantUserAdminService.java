package com.quanlycuahang.erp.platform.service;

import com.quanlycuahang.erp.auth.dto.EmployeeCreateRequest;
import com.quanlycuahang.erp.auth.dto.EmployeeResponse;
import com.quanlycuahang.erp.auth.dto.EmployeeUpdateRequest;
import com.quanlycuahang.erp.auth.dto.RoleSummaryResponse;
import com.quanlycuahang.erp.auth.entity.Role;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.RoleRepository;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import com.quanlycuahang.erp.system.repository.TenantRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin CRUD tai khoan (User) cua BAT KY tenant nao (ho tro chu cua hang khi quen mat khau,
 * hoac tao them tai khoan ho) - phien ban song song voi EmployeeService (tenant User tu quan ly
 * nhan vien CUA MINH) nhung nhan tenantId TUONG MINH qua tham so thay vi dua vao TenantContext, vi
 * request cua Super Admin khong bao gio gan voi 1 tenant nao (xem TenantAdminService).
 *
 * <p>Moi truy van o day PHAI loc tenant_id tuong minh (findByTenantId..., khong phai findAll()) -
 * @Filter cua Hibernate KHONG bat trong request nay (TenantContext null), nen findAll() thuong se
 * tra ve du lieu cua MOI tenant chu khong rieng tenant dang thao tac.
 */
@Service
public class TenantUserAdminService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final BranchRepository branchRepository;
  private final TenantRepository tenantRepository;
  private final PasswordEncoder passwordEncoder;

  public TenantUserAdminService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      BranchRepository branchRepository,
      TenantRepository tenantRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.branchRepository = branchRepository;
    this.tenantRepository = tenantRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<EmployeeResponse> list(Long tenantId) {
    requireTenant(tenantId);
    return userRepository.findByTenantIdOrderByFullNameAsc(tenantId).stream()
        .map(TenantUserAdminService::toResponse)
        .toList();
  }

  @Transactional
  public EmployeeResponse create(Long tenantId, EmployeeCreateRequest request) {
    Tenant tenant = requireTenant(tenantId);
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new BusinessRuleException("EMPLOYEE_DUPLICATE_USERNAME", "Ten dang nhap da ton tai");
    }
    Set<Role> roles = resolveRoles(request.getRoleIds());

    User user = new User();
    user.setTenant(tenant);
    user.setUsername(request.getUsername());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setFullName(request.getFullName());
    user.setPhone(request.getPhone());
    user.setActive(true);
    user.setRoles(roles);
    user.setBranches(new HashSet<>(branchRepository.findByTenantId(tenantId)));

    return toResponse(userRepository.save(user));
  }

  @Transactional
  public EmployeeResponse update(Long tenantId, Long userId, EmployeeUpdateRequest request) {
    User user = requireTenantUser(tenantId, userId);
    if (Boolean.FALSE.equals(request.getActive())) {
      assertNotLastActiveUser(tenantId, user);
    }
    user.setFullName(request.getFullName());
    user.setPhone(request.getPhone());
    user.setActive(request.getActive());
    user.setRoles(resolveRoles(request.getRoleIds()));
    return toResponse(userRepository.save(user));
  }

  @Transactional
  public void deactivate(Long tenantId, Long userId) {
    User user = requireTenantUser(tenantId, userId);
    assertNotLastActiveUser(tenantId, user);
    user.setActive(false);
    userRepository.save(user);
  }

  @Transactional
  public void resetPassword(Long tenantId, Long userId, String newPassword) {
    User user = requireTenantUser(tenantId, userId);
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  private void assertNotLastActiveUser(Long tenantId, User user) {
    if (!user.isActive()) {
      return;
    }
    long activeCount =
        userRepository.findByTenantIdOrderByFullNameAsc(tenantId).stream()
            .filter(User::isActive)
            .count();
    if (activeCount <= 1) {
      throw new BusinessRuleException(
          "TENANT_USER_LAST_ACTIVE",
          "Phai giu lai it nhat 1 tai khoan dang hoat dong cho cua hang nay");
    }
  }

  private Tenant requireTenant(Long tenantId) {
    return tenantRepository
        .findById(tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cua hang"));
  }

  private User requireTenantUser(Long tenantId, Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan"));
    if (!user.getTenant().getId().equals(tenantId)) {
      throw new ResourceNotFoundException("Khong tim thay tai khoan");
    }
    return user;
  }

  private Set<Role> resolveRoles(Set<Long> roleIds) {
    Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
    if (roles.size() != roleIds.size()) {
      throw new BusinessRuleException(
          "EMPLOYEE_INVALID_ROLE", "Mot hoac nhieu vai tro khong ton tai");
    }
    return roles;
  }

  private static EmployeeResponse toResponse(User user) {
    EmployeeResponse response = new EmployeeResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setFullName(user.getFullName());
    response.setPhone(user.getPhone());
    response.setActive(user.isActive());
    response.setRoles(
        user.getRoles().stream()
            .map(
                role -> {
                  RoleSummaryResponse dto = new RoleSummaryResponse();
                  dto.setId(role.getId());
                  dto.setCode(role.getCode());
                  dto.setDisplayName(role.getDisplayName());
                  return dto;
                })
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList());
    return response;
  }
}
