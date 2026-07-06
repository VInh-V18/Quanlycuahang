import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAppSelector } from "@/store/hooks";

function RealtimeClock() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);
  return <span className="tabular-nums">{now.toLocaleTimeString("vi-VN")}</span>;
}

/** Layout POS toàn màn hình, độc lập với Sidebar/Topbar chính (docs/phase4/layout.md) —
 * tối đa vùng thao tác cho thu ngân. Thanh tiêu đề luôn tối màu (jade-900) như Sidebar chính,
 * khớp mockup FruitHouse — không theo theme sáng/tối của phần còn lại. */
export function PosLayout() {
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);

  return (
    <div className="flex h-svh flex-col overflow-hidden bg-background">
      <header className="flex h-12 shrink-0 items-center gap-4 bg-sidebar px-4 text-sm text-sidebar-foreground">
        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-primary text-xs font-bold">
          FH
        </div>
        <span className="font-medium">POS — FruitHouse</span>
        <span className="ml-auto text-sidebar-muted">Thu ngân: {user?.fullName}</span>
        <RealtimeClock />
        <Button
          variant="ghost"
          size="sm"
          onClick={() => navigate("/")}
          className="gap-1 text-sidebar-foreground hover:bg-sidebar-border hover:text-sidebar-foreground"
        >
          <LogOut className="h-4 w-4" />
          Thoát POS
        </Button>
      </header>
      <div className="flex-1 overflow-hidden">
        <Outlet />
      </div>
    </div>
  );
}
