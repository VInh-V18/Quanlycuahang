package com.quanlycuahang.erp.system.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.system.dto.BranchResponse;
import com.quanlycuahang.erp.system.dto.BranchUpdateRequest;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD chi nhanh (FH-16/FH-hoa-don-v2) — tach ra khoi BranchController (truoc day controller nay la
 * ngoai le DUY NHAT trong toan bo he thong goi thang Repository va chua luon quy tac nghiep vu (loc
 * chi nhanh duoc xem, rang buoc "phai con >=1 chi nhanh hoat dong") + @Transactional ngay tren
 * controller, khac voi moi module khac deu theo Controller->Service->Repository - phat hien khi
 * rieng soat kien truc). He thong ban dau chi seed san 1 chi nhanh va CURRENT_BRANCH_ID=1 hardcode
 * o FE (xem PROJECT_STATE) nen luon phai giu lai it nhat 1 chi nhanh dang hoat dong.
 *
 * <p>list() (toan bo chi nhanh cua tenant) duoc cache Caffeine theo tenantId, TTL ngan + xoa chu
 * dong khi ghi (Prompt #7, P2 hieu nang) - danh sach nay doc o hau het moi trang (dieu huong chon
 * chi nhanh) nhung it thay doi. listMine() (chi nhanh RIENG cua 1 user, phu thuoc phan quyen +
 * user_branches) KHONG cache - phu thuoc tung user chu khong chi tenant, loi ich nho vi so chi
 * nhanh/tenant von da rat it, khong dang danh doi rui ro tra nham danh sach cua user khac.
 */
@Service
public class BranchService {

  private final BranchRepository branchRepository;
  private final CurrentUserProvider currentUserProvider;
  private final Cache<Long, List<BranchResponse>> cache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(120)).maximumSize(1000).build();

  public BranchService(BranchRepository branchRepository, CurrentUserProvider currentUserProvider) {
    this.branchRepository = branchRepository;
    this.currentUserProvider = currentUserProvider;
  }

  @Transactional(readOnly = true)
  public List<BranchResponse> list() {
    return cache.get(
        TenantContext.get(),
        tenantId -> branchRepository.findAll().stream().map(BranchService::toResponse).toList());
  }

  /**
   * Danh sach chi nhanh NGUOI DUNG HIEN TAI duoc phep chuyen tren thanh dieu huong (khac voi list()
   * o tren la toan bo he thong, doi hoi quyen branch:view ma cashier/thu kho khong co) — owner/
   * manager thay toan chuoi, cac role con lai chi thay chi nhanh duoc gan (user_branches).
   */
  @Transactional(readOnly = true)
  public List<BranchResponse> listMine() {
    User user = currentUserProvider.requireCurrentUser();
    List<Branch> branches =
        BranchAccessGuard.hasFullAccess(user)
            ? branchRepository.findByActiveTrueOrderByNameAsc()
            : user.getBranches().stream()
                .filter(Branch::isActive)
                .sorted(Comparator.comparing(Branch::getName))
                .toList();
    return branches.stream().map(BranchService::toResponse).toList();
  }

  @Transactional
  public BranchResponse create(BranchUpdateRequest request) {
    Branch branch = new Branch();
    branch.setName(request.getName());
    branch.setAddress(request.getAddress());
    branch.setPhone(request.getPhone());
    branch = branchRepository.save(branch);
    cache.invalidate(TenantContext.get());
    return toResponse(branch);
  }

  @Transactional
  public BranchResponse update(Long id, BranchUpdateRequest request) {
    Branch branch =
        branchRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
    branch.setName(request.getName());
    branch.setAddress(request.getAddress());
    branch.setPhone(request.getPhone());
    branch = branchRepository.save(branch);
    cache.invalidate(TenantContext.get());
    return toResponse(branch);
  }

  @Transactional
  public void delete(Long id) {
    Branch branch =
        branchRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
    if (branchRepository.count() <= 1) {
      throw new BusinessRuleException(
          "BRANCH_LAST_REMAINING", "Phải giữ lại ít nhất 1 chi nhánh đang hoạt động");
    }
    branchRepository.delete(branch);
    cache.invalidate(TenantContext.get());
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
