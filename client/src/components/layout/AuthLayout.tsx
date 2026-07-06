import { Outlet } from "react-router-dom";

/** Layout cho màn đăng nhập/quên mật khẩu — không sidebar/topbar, căn giữa màn hình. */
export function AuthLayout() {
  return (
    <div
      className="flex min-h-svh items-center justify-center p-4"
      style={{ background: "linear-gradient(135deg, hsl(168 59% 11%), hsl(168 70% 22%))" }}
    >
      <div className="w-full max-w-md">
        <Outlet />
      </div>
    </div>
  );
}
