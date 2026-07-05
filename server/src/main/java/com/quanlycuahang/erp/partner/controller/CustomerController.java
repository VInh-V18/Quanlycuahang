package com.quanlycuahang.erp.partner.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.partner.dto.CustomerRequest;
import com.quanlycuahang.erp.partner.dto.CustomerResponse;
import com.quanlycuahang.erp.partner.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('customer:view')")
  public ResponseEntity<ApiResponse<List<CustomerResponse>>> search(
      @RequestParam(required = false, defaultValue = "") String search, Pageable pageable) {
    return ResponseEntity.ok(customerService.search(search, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:view')")
  public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(customerService.getById(id)));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('customer:create')")
  public ResponseEntity<ApiResponse<CustomerResponse>> create(
      @Valid @RequestBody CustomerRequest request) {
    return ResponseEntity.ok(ApiResponse.success(customerService.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('customer:update')")
  public ResponseEntity<ApiResponse<CustomerResponse>> update(
      @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
    return ResponseEntity.ok(ApiResponse.success(customerService.update(id, request)));
  }
}
