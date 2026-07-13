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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin CRUD tai khoan (User) cua BAT KY tenant nao (ho tro chu cua hang khi quen mat khau,
 * hoac tao them tai khoan ho) - phien ban song song voi EmployeeService (tenant User tu quan ly
 * nhan vien CUA MINH) nhung nhan tenantId TUONG MINH qua tham so thay vi dua vao TenantContext, vi
 * request cua Super Admin khong bao gio gan voi 1 tenant nao (xem TenantAdminService).
 *
 * <p>Moi truy van o day PHAI loc tenant_id tuong minh (findByTenantId..., khong phai findAll())
 * - @Filter cua Hibernate KHONG bat trong request nay (TenantContext null), nen findAll() thuong se
 * tra ve du lieu cua MOI tenant chu khong rieng tenant dang thao tac.
 */
@Service
public class TenantUserAdminService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final BranchRepository branchRepository;
  private final TenantRepository tenantRepository;
  private final PasswordEncoder passwordEncoder;
  private final PlatformAuditService platformAuditService;
  private final JdbcTemplate jdbcTemplate;

  public TenantUserAdminService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      BranchRepository branchRepository,
      TenantRepository tenantRepository,
      PasswordEncoder passwordEncoder,
      PlatformAuditService platformAuditService,
      JdbcTemplate jdbcTemplate) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.branchRepository = branchRepository;
    this.tenantRepository = tenantRepository;
    this.passwordEncoder = passwordEncoder;
    this.platformAuditService = platformAuditService;
    this.jdbcTemplate = jdbcTemplate;
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
      throw new BusinessRuleException("EMPLOYEE_DUPLICATE_USERNAME", "Tên đăng nhập đã tồn tại");
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

    EmployeeResponse response = toResponse(userRepository.save(user));
    platformAuditService.record(
        "TENANT_USER_CREATE", tenantId, tenant.getName() + " / " + user.getUsername(), null);
    return response;
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
    EmployeeResponse response = toResponse(userRepository.save(user));
    platformAuditService.record(
        "TENANT_USER_UPDATE",
        tenantId,
        user.getUsername(),
        java.util.Map.of("active", user.isActive()));
    return response;
  }

  @Transactional
  public void deactivate(Long tenantId, Long userId) {
    User user = requireTenantUser(tenantId, userId);
    assertNotLastActiveUser(tenantId, user);
    user.setActive(false);
    userRepository.save(user);
    platformAuditService.record("TENANT_USER_DEACTIVATE", tenantId, user.getUsername(), null);
  }

  /**
   * XOA VINH VIEN tai khoan - khac han deactivate() (chi khoa dang nhap, giu nguyen du lieu). Dung
   * raw SQL (JdbcTemplate) chu KHONG phai userRepository.delete(): User co @SQLDelete (chi UPDATE
   * deleted_at), goi qua JPA se chi "vo hieu hoa" chu khong xoa that. Neu tai khoan da tung phat
   * sinh hoat dong that (don hang/ca lam viec/phieu nhap/nhat ky...), DB tu choi bang FK constraint
   * - bat loi do va dich thanh thong bao ro thay vi de exception tho thoat ra; day la CHU DICH,
   * KHONG phai thieu sot: xoa 1 tai khoan da tao ra du lieu tai chinh se lam mo coi du lieu do vinh
   * vien.
   */
  @Transactional
  public void delete(Long tenantId, Long userId) {
    User user = requireTenantUser(tenantId, userId);
    assertNotLastActiveUser(tenantId, user);
    String username = user.getUsername();

    jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
    jdbcTemplate.update("DELETE FROM user_branches WHERE user_id = ?", userId);
    try {
      jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    } catch (DataIntegrityViolationException ex) {
      throw new BusinessRuleException(
          "TENANT_USER_HAS_ACTIVITY",
          "Tài khoản đã phát sinh hoạt động (đơn hàng, ca làm việc, phiếu nhập...), chỉ có thể vô"
              + " hiệu hóa, không thể xóa vĩnh viễn");
    }

    platformAuditService.record("TENANT_USER_DELETE", tenantId, username, null);
  }

  @Transactional
  public void resetPassword(Long tenantId, Long userId, String newPassword) {
    User user = requireTenantUser(tenantId, userId);
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    // Hanh dong nhay cam nhat (Super Admin co the truy cap bat ky tai khoan tenant nao qua day) -
    // KHONG ghi mat khau moi vao detail, chi ghi hanh dong da xay ra.
    platformAuditService.record("TENANT_USER_RESET_PASSWORD", tenantId, user.getUsername(), null);
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
          "Phải giữ lại ít nhất 1 tài khoản đang hoạt động cho cửa hàng này");
    }
  }

  private Tenant requireTenant(Long tenantId) {
    return tenantRepository
        .findById(tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng"));
  }

  private User requireTenantUser(Long tenantId, Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    if (!user.getTenant().getId().equals(tenantId)) {
      throw new ResourceNotFoundException("Không tìm thấy tài khoản");
    }
    return user;
  }

  private Set<Role> resolveRoles(Set<Long> roleIds) {
    Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
    if (roles.size() != roleIds.size()) {
      throw new BusinessRuleException(
          "EMPLOYEE_INVALID_ROLE", "Một hoặc nhiều vai trò không tồn tại");
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
