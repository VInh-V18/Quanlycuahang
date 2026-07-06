package com.quanlycuahang.erp.auth.security;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.exception.PermissionDeniedException;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Chan IDOR da chi nhanh (Phase 1.1 quy tac 4, bang user_branches): moi @PreAuthorize hien tai chi
 * kiem tra role:action (vd order:view), khong doi chieu branchId trong request voi chi nhanh nguoi
 * dung thuc su duoc gan. Component nay la lop kiem tra bo sung, goi rieng o Controller/Service noi
 * biet branchId (tham so request hoac sau khi load entity), phat hien khi rieng soat (senior code
 * review): thu ngan chi nhanh A doi branchId sang B tren query param se xem duoc du lieu chi nhanh
 * khac ma khong can quyen gi them.
 */
@Component
public class BranchAccessGuard {

  /** owner/manager quan ly toan chuoi, khong bi gioi han theo user_branches. */
  private static final Set<String> FULL_ACCESS_ROLE_CODES = Set.of("owner", "manager");

  private final CurrentUserProvider currentUserProvider;

  public BranchAccessGuard(CurrentUserProvider currentUserProvider) {
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * @param branchId chi nhanh muc tieu cua request (null nghia la "tat ca chi nhanh" — chi cho phep
   *     role toan quyen).
   * @throws PermissionDeniedException neu user hien tai khong duoc gan chi nhanh nay va khong co
   *     role toan quyen.
   */
  public void assertAccess(Long branchId) {
    User user = currentUserProvider.requireCurrentUser();
    if (hasFullAccess(user)) {
      return;
    }
    if (branchId == null) {
      throw new PermissionDeniedException("Vui long chon 1 chi nhanh cu the");
    }
    boolean allowed = user.getBranches().stream().anyMatch(b -> b.getId().equals(branchId));
    if (!allowed) {
      throw new PermissionDeniedException("Ban khong co quyen truy cap chi nhanh nay");
    }
  }

  private static boolean hasFullAccess(User user) {
    return user.getRoles().stream()
        .anyMatch(role -> FULL_ACCESS_ROLE_CODES.contains(role.getCode()));
  }
}
