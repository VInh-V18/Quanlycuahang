import type { ReactNode } from "react";
import { useAppSelector } from "@/store/hooks";

export interface PermissionGateProps {
  /** Một quyền ("product:view") hoặc nhiều quyền — mặc định cần đủ 1 trong số đó (OR). */
  perm: string | string[];
  /** true = cần đủ TẤT CẢ quyền trong mảng (AND) thay vì chỉ cần 1. */
  requireAll?: boolean;
  children: ReactNode;
  fallback?: ReactNode;
}

/** Ẩn/hiện phần UI theo quyền `resource:action` của user hiện tại (đồng bộ ma trận quyền
 * Phase 1) — chỉ là UX, Backend luôn là nguồn kiểm soát quyền cuối cùng qua @PreAuthorize. */
export function PermissionGate({ perm, requireAll = false, children, fallback = null }: PermissionGateProps) {
  const permissions = useAppSelector((state) => state.auth.permissions);
  const required = Array.isArray(perm) ? perm : [perm];

  const allowed = requireAll
    ? required.every((p) => permissions.includes(p))
    : required.some((p) => permissions.includes(p));

  return allowed ? <>{children}</> : <>{fallback}</>;
}
