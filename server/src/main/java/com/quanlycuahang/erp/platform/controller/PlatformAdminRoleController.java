package com.quanlycuahang.erp.platform.controller;

import com.quanlycuahang.erp.auth.dto.PermissionResponse;
import com.quanlycuahang.erp.auth.dto.RoleResponse;
import com.quanlycuahang.erp.auth.dto.UpdateRolePermissionsRequest;
import com.quanlycuahang.erp.auth.service.RoleService;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh sach + sua ma tran quyen (global, dung chung moi tenant) - CHI Super Admin duoc goi.
 *
 * <p>Thao tac SUA (updatePermissions) truoc day nam o RoleController (/api/v1/roles/{id}/
 * permissions), cho phep bat ky chu cua hang nao co quyen employee:manage-permission TRONG TENANT
 * cua ho goi duoc - vi Role/Permission la du lieu TOAN CUC (khong ke thua TenantScopedEntity), 1
 * chu cua hang doi quyen cua "Thu ngan" se doi luon cho MOI tenant khac tren he thong. Chuyen han
 * ve day (chi Super Admin) de dong lo hong nay - tenant User gio chi con xem duoc ma tran quyen
 * (RoleController.listRoles/listPermissions), khong sua duoc nua.
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

  /**
   * Danh sach TOAN BO permission - can cho UI ma tran phan quyen cua Super Admin. Khong dung lai
   * duoc GET /api/v1/permissions cua tenant vi 2 loai token co chu dinh KHONG dung lan cua nhau
   * (PlatformAdminJwtAuthenticationFilter tu choi token tenant va nguoc lai).
   */
  @GetMapping("/permissions")
  public ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions() {
    return ResponseEntity.ok(ApiResponse.success(roleService.listPermissions()));
  }

  @PutMapping("/{id}/permissions")
  public ResponseEntity<ApiResponse<RoleResponse>> updatePermissions(
      @PathVariable Long id, @Valid @RequestBody UpdateRolePermissionsRequest request) {
    return ResponseEntity.ok(ApiResponse.success(roleService.updatePermissions(id, request)));
  }
}
