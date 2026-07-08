package com.quanlycuahang.erp.platform.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.platform.dto.TenantActiveUpdateRequest;
import com.quanlycuahang.erp.platform.dto.TenantCreateRequest;
import com.quanlycuahang.erp.platform.dto.TenantResponse;
import com.quanlycuahang.erp.platform.service.TenantAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tao/xem/khoa Tenant (cua hang) - chi Super Admin goi duoc (SecurityConfig da bat buoc authority
 * PLATFORM_ADMIN cho toan bo /api/v1/platform-admin/**).
 */
@RestController
@RequestMapping("/api/v1/platform-admin/tenants")
public class TenantAdminController {

  private final TenantAdminService tenantAdminService;

  public TenantAdminController(TenantAdminService tenantAdminService) {
    this.tenantAdminService = tenantAdminService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<TenantResponse>>> list() {
    return ResponseEntity.ok(ApiResponse.success(tenantAdminService.listTenants()));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<TenantResponse>> create(
      @Valid @RequestBody TenantCreateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(tenantAdminService.createTenant(request)));
  }

  @PutMapping("/{id}/active")
  public ResponseEntity<ApiResponse<TenantResponse>> setActive(
      @PathVariable Long id, @Valid @RequestBody TenantActiveUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(tenantAdminService.setActive(id, request.getActive())));
  }
}
