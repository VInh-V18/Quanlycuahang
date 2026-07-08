package com.quanlycuahang.erp.platform.controller;

import com.quanlycuahang.erp.auth.dto.RoleResponse;
import com.quanlycuahang.erp.auth.service.RoleService;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh sach vai tro (global, dung chung moi tenant) de Super Admin chon khi tao/sua tai khoan cho
 * 1 tenant (TenantUserAdminController) - endpoint /api/v1/roles thuong yeu cau quyen employee:view
 * (tenant User), Super Admin khong co quyen do nen can duong rieng.
 */
@RestController
@RequestMapping("/api/v1/platform-admin/roles")
public class PlatformAdminRoleController {

  private final RoleService roleService;

  public PlatformAdminRoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<RoleResponse>>> list() {
    return ResponseEntity.ok(ApiResponse.success(roleService.listRoles()));
  }
}
