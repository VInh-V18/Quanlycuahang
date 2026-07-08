package com.quanlycuahang.erp.platform.controller;

import com.quanlycuahang.erp.auth.dto.EmployeeCreateRequest;
import com.quanlycuahang.erp.auth.dto.EmployeeResponse;
import com.quanlycuahang.erp.auth.dto.EmployeeUpdateRequest;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.platform.dto.AdminResetPasswordRequest;
import com.quanlycuahang.erp.platform.service.TenantUserAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Super Admin CRUD tai khoan cua 1 tenant cu the (ho tro chu cua hang: quen mat khau, can them
 * tai khoan...) - chi Super Admin goi duoc (SecurityConfig da bat buoc authority PLATFORM_ADMIN
 * cho toan bo /api/v1/platform-admin/**).
 */
@RestController
@RequestMapping("/api/v1/platform-admin/tenants/{tenantId}/users")
public class TenantUserAdminController {

  private final TenantUserAdminService tenantUserAdminService;

  public TenantUserAdminController(TenantUserAdminService tenantUserAdminService) {
    this.tenantUserAdminService = tenantUserAdminService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<EmployeeResponse>>> list(@PathVariable Long tenantId) {
    return ResponseEntity.ok(ApiResponse.success(tenantUserAdminService.list(tenantId)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<EmployeeResponse>> create(
      @PathVariable Long tenantId, @Valid @RequestBody EmployeeCreateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(tenantUserAdminService.create(tenantId, request)));
  }

  @PutMapping("/{userId}")
  public ResponseEntity<ApiResponse<EmployeeResponse>> update(
      @PathVariable Long tenantId,
      @PathVariable Long userId,
      @Valid @RequestBody EmployeeUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(tenantUserAdminService.update(tenantId, userId, request)));
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<ApiResponse<Void>> deactivate(
      @PathVariable Long tenantId, @PathVariable Long userId) {
    tenantUserAdminService.deactivate(tenantId, userId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/{userId}/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @PathVariable Long tenantId,
      @PathVariable Long userId,
      @Valid @RequestBody AdminResetPasswordRequest request) {
    tenantUserAdminService.resetPassword(tenantId, userId, request.getNewPassword());
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
