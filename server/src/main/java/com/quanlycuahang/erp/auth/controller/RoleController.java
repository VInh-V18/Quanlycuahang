package com.quanlycuahang.erp.auth.controller;

import com.quanlycuahang.erp.auth.dto.PermissionResponse;
import com.quanlycuahang.erp.auth.dto.RoleResponse;
import com.quanlycuahang.erp.auth.dto.UpdateRolePermissionsRequest;
import com.quanlycuahang.erp.auth.service.RoleService;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ma tran phan quyen — xem/sua tap quyen cua tung vai tro (FH-13). */
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

  @PutMapping("/roles/{id}/permissions")
  @PreAuthorize("hasAuthority('employee:manage-permission')")
  public ResponseEntity<ApiResponse<RoleResponse>> updatePermissions(
      @PathVariable Long id, @Valid @RequestBody UpdateRolePermissionsRequest request) {
    return ResponseEntity.ok(ApiResponse.success(roleService.updatePermissions(id, request)));
  }
}
