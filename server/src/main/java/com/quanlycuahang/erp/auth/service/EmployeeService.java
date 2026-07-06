package com.quanlycuahang.erp.auth.service;

import com.quanlycuahang.erp.auth.dto.EmployeeCreateRequest;
import com.quanlycuahang.erp.auth.dto.EmployeeResponse;
import com.quanlycuahang.erp.auth.dto.EmployeeUpdateRequest;
import com.quanlycuahang.erp.auth.dto.RoleSummaryResponse;
import com.quanlycuahang.erp.auth.entity.Role;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.RoleRepository;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD nhan vien (FH-13) — tao/sua nhan vien va gan vai tro (roles). Chi nhanh: he thong hien chi
 * co 1 chi nhanh seed san (xem CURRENT_BRANCH_ID o Frontend), nen nhan vien moi duoc gan vao toan
 * bo chi nhanh dang co, giong cach V2__seed_data.sql lam voi 3 user seed.
 */
@Service
public class EmployeeService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final BranchRepository branchRepository;
  private final PasswordEncoder passwordEncoder;
  private final CurrentUserProvider currentUserProvider;

  public EmployeeService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      BranchRepository branchRepository,
      PasswordEncoder passwordEncoder,
      CurrentUserProvider currentUserProvider) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.branchRepository = branchRepository;
    this.passwordEncoder = passwordEncoder;
    this.currentUserProvider = currentUserProvider;
  }

  @Transactional(readOnly = true)
  public List<EmployeeResponse> list() {
    return userRepository.findAllByOrderByFullNameAsc().stream()
        .map(EmployeeService::toResponse)
        .toList();
  }

  @Transactional
  public EmployeeResponse create(EmployeeCreateRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new BusinessRuleException("EMPLOYEE_DUPLICATE_USERNAME", "Ten dang nhap da ton tai");
    }
    Set<Role> roles = resolveRoles(request.getRoleIds());

    User user = new User();
    user.setUsername(request.getUsername());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setFullName(request.getFullName());
    user.setPhone(request.getPhone());
    user.setActive(true);
    user.setRoles(roles);
    user.setBranches(new HashSet<>(branchRepository.findAll()));

    return toResponse(userRepository.save(user));
  }

  @Transactional
  public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien"));

    if (Boolean.FALSE.equals(request.getActive())
        && currentUserProvider.getCurrentUser().map(User::getId).map(id::equals).orElse(false)) {
      throw new BusinessRuleException(
          "EMPLOYEE_CANNOT_DEACTIVATE_SELF", "Khong the tu vo hieu hoa tai khoan dang dang nhap");
    }

    user.setFullName(request.getFullName());
    user.setPhone(request.getPhone());
    user.setActive(request.getActive());
    user.setRoles(resolveRoles(request.getRoleIds()));

    return toResponse(userRepository.save(user));
  }

  @Transactional
  public void deactivate(Long id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien"));

    if (currentUserProvider.getCurrentUser().map(User::getId).map(id::equals).orElse(false)) {
      throw new BusinessRuleException(
          "EMPLOYEE_CANNOT_DEACTIVATE_SELF", "Khong the tu vo hieu hoa tai khoan dang dang nhap");
    }

    user.setActive(false);
    userRepository.save(user);
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
