package com.quanlycuahang.erp.partner.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.partner.dto.CustomerGroupRequest;
import com.quanlycuahang.erp.partner.dto.CustomerGroupResponse;
import com.quanlycuahang.erp.partner.service.CustomerGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quan ly nhom khach hang (VIP/Than thiet/Doanh nghiep/Moi...) — dung cho bo loc + form o trang
 * Khach hang (FH-11). CRUD dung chung quyen voi Khach hang (khong tach quyen rieng).
 */
@RestController
@RequestMapping("/api/v1/customer-groups")
public class CustomerGroupController {

  private final CustomerGroupService customerGroupService;

  public CustomerGroupController(CustomerGroupService customerGroupService) {
    this.customerGroupService = customerGroupService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('customer:view')")
  public ResponseEntity<ApiResponse<List<CustomerGroupResponse>>> list() {
    return ResponseEntity.ok(ApiResponse.success(customerGroupService.list()));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('customer:create')")
  public ResponseEntity<ApiResponse<CustomerGroupResponse>> create(
      @Valid @RequestBody CustomerGroupRequest request) {
    return ResponseEntity.ok(ApiResponse.success(customerGroupService.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:update')")
  public ResponseEntity<ApiResponse<CustomerGroupResponse>> update(
      @PathVariable Long id, @Valid @RequestBody CustomerGroupRequest request) {
    return ResponseEntity.ok(ApiResponse.success(customerGroupService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:update')")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    customerGroupService.delete(id);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
