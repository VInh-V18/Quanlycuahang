import { useAppSelector } from "@/store/hooks";
import { CURRENT_BRANCH_ID } from "@/lib/constants";

/** Chi nhanh dang chon o Topbar — moi trang can loc theo chi nhanh phai dung hook nay thay vi hang
 * cung CURRENT_BRANCH_ID truc tiep. Fallback ve CURRENT_BRANCH_ID trong khoanh khac ngan truoc khi
 * Topbar nap xong danh sach chi nhanh cua user va ghi state (xem Topbar.tsx). */
export function useCurrentBranchId(): number {
  const currentBranchId = useAppSelector((state) => state.ui.currentBranchId);
  return currentBranchId ?? CURRENT_BRANCH_ID;
}
