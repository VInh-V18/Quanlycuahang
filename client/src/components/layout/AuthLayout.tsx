import { Outlet } from "react-router-dom";

/** Layout cho màn đăng nhập/quên mật khẩu — không sidebar/topbar, căn giữa màn hình. */
export function AuthLayout() {
  return (
    <div className="flex min-h-svh items-center justify-center bg-muted p-4">
      <div className="w-full max-w-sm">
        <Outlet />
      </div>
    </div>
  );
}
