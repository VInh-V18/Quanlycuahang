package com.quanlycuahang.erp.auth.service;

import com.quanlycuahang.erp.auth.dto.PermissionResponse;
import com.quanlycuahang.erp.auth.dto.RoleResponse;
import com.quanlycuahang.erp.auth.dto.UpdateRolePermissionsRequest;
import com.quanlycuahang.erp.auth.entity.Permission;
import com.quanlycuahang.erp.auth.entity.Role;
import com.quanlycuahang.erp.auth.repository.PermissionRepository;
import com.quanlycuahang.erp.auth.repository.RoleRepository;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ma tran phan quyen (FH-13): xem 6 vai tro seed san (V2__seed_data.sql) va sua tap quyen
 * (permissions) cua tung vai tro — hien thi dang luoi resource:action x vai tro o Frontend.
 */
@Service
public class RoleService {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;

  public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
  }

  @Transactional(readOnly = true)
  public List<RoleResponse> listRoles() {
    return roleRepository.findAllByOrderByIdAsc().stream().map(RoleService::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<PermissionResponse> listPermissions() {
    return permissionRepository.findAllByOrderByCodeAsc().stream()
        .map(
            permission -> {
              PermissionResponse dto = new PermissionResponse();
              dto.setId(permission.getId());
              dto.setCode(permission.getCode());
              dto.setDescription(permission.getDescription());
              return dto;
            })
        .toList();
  }

  @Transactional
  public RoleResponse updatePermissions(Long roleId, UpdateRolePermissionsRequest request) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay vai tro"));

    if ("owner".equals(role.getCode())
        && !request.getPermissionCodes().contains("employee:manage-permission")) {
      throw new BusinessRuleException(
          "ROLE_OWNER_MUST_KEEP_PERMISSION_MANAGEMENT",
          "Vai tro Chu cua hang phai luon giu quyen 'Doi vai tro/quyen cua nhan vien' de tranh tu khoa quyen truy cap");
    }

    List<Permission> permissions = permissionRepository.findByCodeIn(request.getPermissionCodes());
    if (permissions.size() != request.getPermissionCodes().size()) {
      Set<String> found = permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
      Set<String> missing = new HashSet<>(request.getPermissionCodes());
      missing.removeAll(found);
      throw new BusinessRuleException(
          "ROLE_INVALID_PERMISSION", "Ma quyen khong ton tai: " + missing);
    }

    role.setPermissions(new HashSet<>(permissions));
    return toResponse(roleRepository.save(role));
  }

  private static RoleResponse toResponse(Role role) {
    RoleResponse dto = new RoleResponse();
    dto.setId(role.getId());
    dto.setCode(role.getCode());
    dto.setDisplayName(role.getDisplayName());
    dto.setPermissionCodes(
        role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()));
    return dto;
  }
}
