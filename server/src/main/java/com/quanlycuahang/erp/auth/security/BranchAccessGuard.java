package com.quanlycuahang.erp.auth.security;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.exception.PermissionDeniedException;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Chan IDOR da chi nhanh (Phase 1.1 quy tac 4, bang user_branches): moi @PreAuthorize hien tai chi
 * kiem tra role:action (vd order:view), khong doi chieu branchId trong request voi chi nhanh nguoi
 * dung thuc su duoc gan. Component nay la lop kiem tra bo sung, goi rieng o Controller/Service noi
 * biet branchId (tham so request hoac sau khi load entity), phat hien khi rieng soat (senior code
 * review): thu ngan chi nhanh A doi branchId sang B tren query param se xem duoc du lieu chi nhanh
 * khac ma khong can quyen gi them.
 *
 * <p>Multi-tenant (B4 mo rong): owner/manager "toan quyen" chi la toan quyen TRONG TENANT CUA HO —
 * truoc khi xet owner/manager hay user_branches, PHAI xac nhan branchId do thuoc dung tenant hien
 * tai. Branch da ke thua TenantScopedEntity (@Filter tu dong theo tenant_id) nen chi can goi
 * existsById binh thuong — Hibernate tu tra false neu branchId thuoc tenant khac, khong can tu so
 * sanh tenant_id thu cong.
 */
@Component
public class BranchAccessGuard {

  /** owner/manager quan ly toan chuoi (trong PHAM VI tenant cua ho), khong bi gioi han theo
   * user_branches. */
  private static final Set<String> FULL_ACCESS_ROLE_CODES = Set.of("owner", "manager");

  private final CurrentUserProvider currentUserProvider;
  private final BranchRepository branchRepository;

  public BranchAccessGuard(
      CurrentUserProvider currentUserProvider, BranchRepository branchRepository) {
    this.currentUserProvider = currentUserProvider;
    this.branchRepository = branchRepository;
  }

  /**
   * @param branchId chi nhanh muc tieu cua request (null nghia la "tat ca chi nhanh" — chi cho phep
   *     role toan quyen).
   * @throws PermissionDeniedException neu user hien tai khong duoc gan chi nhanh nay va khong co
   *     role toan quyen.
   */
  public void assertAccess(Long branchId) {
    User user = currentUserProvider.requireCurrentUser();
    if (branchId == null) {
      if (hasFullAccess(user)) {
        return;
      }
      throw new PermissionDeniedException("Vui long chon 1 chi nhanh cu the");
    }
    // Branch.@Filter tu dong loai tru chi nhanh khac tenant — existsById tra false ca khi branchId
    // co that (thuoc tenant khac) lan khi khong ton tai, dung y: khong duoc phep tiet lo chi nhanh
    // do co ton tai hay khong o he thong khac.
    if (!branchRepository.existsById(branchId)) {
      throw new PermissionDeniedException("Khong tim thay chi nhanh nay");
    }
    if (hasFullAccess(user)) {
      return;
    }
    boolean allowed = user.getBranches().stream().anyMatch(b -> b.getId().equals(branchId));
    if (!allowed) {
      throw new PermissionDeniedException("Ban khong co quyen truy cap chi nhanh nay");
    }
  }

  /**
   * owner/manager quan ly toan chuoi — dung lai o cac Service can phan biet "quan ly" vs "nhan vien
   * thuong" (vd ShiftService: cashier chi xem duoc ca cua chinh minh).
   */
  public static boolean hasFullAccess(User user) {
    return user.getRoles().stream()
        .anyMatch(role -> FULL_ACCESS_ROLE_CODES.contains(role.getCode()));
  }
}
