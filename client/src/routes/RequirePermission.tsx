import type { ReactNode } from "react";
import { useAppSelector } from "@/store/hooks";
import { ForbiddenPage } from "@/pages/ForbiddenPage";

export interface RequirePermissionProps {
  perm: string | string[];
  requireAll?: boolean;
  children: ReactNode;
}

/** Route guard theo quyền `resource:action` — dùng trong router.tsx bọc quanh route cần quyền
 * cụ thể (khác PermissionGate ở chỗ đây chặn cả trang, có trang 403 riêng). */
export function RequirePermission({ perm, requireAll = false, children }: RequirePermissionProps) {
  const permissions = useAppSelector((state) => state.auth.permissions);
  const required = Array.isArray(perm) ? perm : [perm];

  const allowed = requireAll
    ? required.every((p) => permissions.includes(p))
    : required.some((p) => permissions.includes(p));

  if (!allowed) {
    return <ForbiddenPage />;
  }

  return <>{children}</>;
}
