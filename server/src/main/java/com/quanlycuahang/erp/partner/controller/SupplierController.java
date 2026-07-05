package com.quanlycuahang.erp.partner.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.partner.dto.SupplierRequest;
import com.quanlycuahang.erp.partner.dto.SupplierResponse;
import com.quanlycuahang.erp.partner.service.SupplierService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

  private final SupplierService supplierService;

  public SupplierController(SupplierService supplierService) {
    this.supplierService = supplierService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('supplier:view')")
  public ResponseEntity<ApiResponse<List<SupplierResponse>>> list(Pageable pageable) {
    return ResponseEntity.ok(supplierService.list(pageable));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('supplier:manage')")
  public ResponseEntity<ApiResponse<SupplierResponse>> create(
      @Valid @RequestBody SupplierRequest request) {
    return ResponseEntity.ok(ApiResponse.success(supplierService.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('supplier:manage')")
  public ResponseEntity<ApiResponse<SupplierResponse>> update(
      @PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
    return ResponseEntity.ok(ApiResponse.success(supplierService.update(id, request)));
  }
}
