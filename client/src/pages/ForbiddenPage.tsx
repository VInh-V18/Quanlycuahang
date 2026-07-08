import { ShieldAlert } from "lucide-react";

export function ForbiddenPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-2 py-24 text-center">
      <ShieldAlert className="h-10 w-10 text-destructive" />
      <h1 className="text-xl font-semibold">Không có quyền truy cập</h1>
      <p className="text-muted-foreground">
        Tài khoản của bạn không có quyền xem trang này. Liên hệ quản lý nếu cần hỗ trợ.
      </p>
    </div>
  );
}
