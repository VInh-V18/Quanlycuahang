package com.quanlycuahang.erp.auth.controller;

import com.quanlycuahang.erp.auth.dto.PermissionResponse;
import com.quanlycuahang.erp.auth.dto.RoleResponse;
import com.quanlycuahang.erp.auth.service.RoleService;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ma tran phan quyen — CHI DOC cho tenant User (FH-13). Role/Permission la du lieu TOAN CUC (dung
 * chung moi tenant, khong ke thua TenantScopedEntity) - truoc day co them PUT /roles/{id}/
 * permissions o day, cho phep BAT KY chu cua hang nao (co quyen employee:manage-permission trong
 * TENANT cua ho) sua thang bang role_permissions dung chung, anh huong toi MOI tenant khac tren he
 * thong - lo hong bao mat nghiem trong phat hien khi rieng soat toan bo codebase. Thao tac SUA da
 * chuyen sang rieng cho Super Admin, xem PlatformAdminRoleController.
 */
@RestController
@RequestMapping("/api/v1")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping("/roles")
  @PreAuthorize("hasAuthority('employee:view')")
  public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
    return ResponseEntity.ok(ApiResponse.success(roleService.listRoles()));
  }

  @GetMapping("/permissions")
  @PreAuthorize("hasAuthority('employee:view')")
  public ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions() {
    return ResponseEntity.ok(ApiResponse.success(roleService.listPermissions()));
  }
}
