package com.quanlycuahang.erp.system.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.system.dto.BranchResponse;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh sach chi nhanh (chi doc) — dung cho tab "Cua hang & chi nhanh" trong Cai dat (FH-16). He
 * thong hien chi co 1 chi nhanh seed san va CURRENT_BRANCH_ID=1 hardcode o FE (xem PROJECT_STATE),
 * nen chua lam CRUD them/sua/xoa chi nhanh o day — ngoai pham vi FH-16.
 */
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

  private final BranchRepository branchRepository;

  public BranchController(BranchRepository branchRepository) {
    this.branchRepository = branchRepository;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('branch:view')")
  public ResponseEntity<ApiResponse<List<BranchResponse>>> list() {
    List<BranchResponse> branches =
        branchRepository.findAll().stream().map(BranchController::toResponse).toList();
    return ResponseEntity.ok(ApiResponse.success(branches));
  }

  private static BranchResponse toResponse(Branch branch) {
    BranchResponse dto = new BranchResponse();
    dto.setId(branch.getId());
    dto.setName(branch.getName());
    dto.setAddress(branch.getAddress());
    dto.setPhone(branch.getPhone());
    dto.setActive(branch.isActive());
    return dto;
  }
}
