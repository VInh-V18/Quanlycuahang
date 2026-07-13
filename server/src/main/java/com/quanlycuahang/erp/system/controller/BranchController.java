package com.quanlycuahang.erp.system.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.system.dto.BranchResponse;
import com.quanlycuahang.erp.system.dto.BranchUpdateRequest;
import com.quanlycuahang.erp.system.service.BranchService;
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

/** CRUD chi nhanh, dung cho tab "Cua hang & chi nhanh" trong Cai dat (FH-16/FH-hoa-don-v2). */
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

  private final BranchService branchService;

  public BranchController(BranchService branchService) {
    this.branchService = branchService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('branch:view')")
  public ResponseEntity<ApiResponse<List<BranchResponse>>> list() {
    return ResponseEntity.ok(ApiResponse.success(branchService.list()));
  }

  @GetMapping("/mine")
  public ResponseEntity<ApiResponse<List<BranchResponse>>> listMine() {
    return ResponseEntity.ok(ApiResponse.success(branchService.listMine()));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('branch:manage')")
  public ResponseEntity<ApiResponse<BranchResponse>> create(
      @Valid @RequestBody BranchUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(branchService.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('branch:manage')")
  public ResponseEntity<ApiResponse<BranchResponse>> update(
      @PathVariable Long id, @Valid @RequestBody BranchUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(branchService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('branch:manage')")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    branchService.delete(id);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
