package com.quanlycuahang.erp.system.controller;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.system.dto.BranchResponse;
import com.quanlycuahang.erp.system.dto.BranchUpdateRequest;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD chi nhanh, dung cho tab "Cua hang & chi nhanh" trong Cai dat (FH-16/FH-hoa-don-v2). He
 * thong ban dau chi seed san 1 chi nhanh va CURRENT_BRANCH_ID=1 hardcode o FE (xem PROJECT_STATE)
 * nen luon phai giu lai it nhat 1 chi nhanh dang hoat dong — xoa chi nhanh cuoi cung se bi chan.
 */
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

  private final BranchRepository branchRepository;
  private final CurrentUserProvider currentUserProvider;

  public BranchController(
      BranchRepository branchRepository, CurrentUserProvider currentUserProvider) {
    this.branchRepository = branchRepository;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('branch:view')")
  public ResponseEntity<ApiResponse<List<BranchResponse>>> list() {
    List<BranchResponse> branches =
        branchRepository.findAll().stream().map(BranchController::toResponse).toList();
    return ResponseEntity.ok(ApiResponse.success(branches));
  }

  /**
   * Danh sach chi nhanh NGUOI DUNG HIEN TAI duoc phep chuyen tren thanh dieu huong (khac voi list()
   * o tren la toan bo he thong, doi hoi quyen branch:view ma cashier/thu kho khong co) — owner/
   * manager thay toan chuoi, cac role con lai chi thay chi nhanh duoc gan (user_branches).
   */
  @GetMapping("/mine")
  @Transactional(readOnly = true)
  public ResponseEntity<ApiResponse<List<BranchResponse>>> listMine() {
    User user = currentUserProvider.requireCurrentUser();
    List<Branch> branches =
        BranchAccessGuard.hasFullAccess(user)
            ? branchRepository.findByActiveTrueOrderByNameAsc()
            : user.getBranches().stream()
                .filter(Branch::isActive)
                .sorted(Comparator.comparing(Branch::getName))
                .toList();
    return ResponseEntity.ok(
        ApiResponse.success(branches.stream().map(BranchController::toResponse).toList()));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('branch:manage')")
  @Transactional
  public ResponseEntity<ApiResponse<BranchResponse>> create(
      @Valid @RequestBody BranchUpdateRequest request) {
    Branch branch = new Branch();
    branch.setName(request.getName());
    branch.setAddress(request.getAddress());
    branch.setPhone(request.getPhone());
    branch = branchRepository.save(branch);
    return ResponseEntity.ok(ApiResponse.success(toResponse(branch)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('branch:manage')")
  @Transactional
  public ResponseEntity<ApiResponse<BranchResponse>> update(
      @PathVariable Long id, @Valid @RequestBody BranchUpdateRequest request) {
    Branch branch =
        branchRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chi nhanh"));
    branch.setName(request.getName());
    branch.setAddress(request.getAddress());
    branch.setPhone(request.getPhone());
    branch = branchRepository.save(branch);
    return ResponseEntity.ok(ApiResponse.success(toResponse(branch)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('branch:manage')")
  @Transactional
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    Branch branch =
        branchRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chi nhanh"));
    if (branchRepository.count() <= 1) {
      throw new BusinessRuleException(
          "BRANCH_LAST_REMAINING", "Phai giu lai it nhat 1 chi nhanh dang hoat dong");
    }
    branchRepository.delete(branch);
    return ResponseEntity.ok(ApiResponse.success(null));
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
