package com.quanlycuahang.erp.auth.controller;

import com.quanlycuahang.erp.auth.dto.EmployeeCreateRequest;
import com.quanlycuahang.erp.auth.dto.EmployeeResponse;
import com.quanlycuahang.erp.auth.dto.EmployeeUpdateRequest;
import com.quanlycuahang.erp.auth.service.EmployeeService;
import com.quanlycuahang.erp.common.dto.ApiResponse;
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

/** Quan ly nhan vien (FH-13). */
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

  private final EmployeeService employeeService;

  public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('employee:view')")
  public ResponseEntity<ApiResponse<List<EmployeeResponse>>> list() {
    return ResponseEntity.ok(ApiResponse.success(employeeService.list()));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('employee:create')")
  public ResponseEntity<ApiResponse<EmployeeResponse>> create(
      @Valid @RequestBody EmployeeCreateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(employeeService.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('employee:update')")
  public ResponseEntity<ApiResponse<EmployeeResponse>> update(
      @PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(employeeService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('employee:delete')")
  public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
    employeeService.deactivate(id);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
