import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
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
 * tối đa vùng thao tác cho thu ngân. */
export function PosLayout() {
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);

  return (
    <div className="flex h-svh flex-col overflow-hidden bg-background">
      <header className="flex h-12 shrink-0 items-center gap-4 border-b bg-background px-4 text-sm">
        <Button variant="ghost" size="sm" onClick={() => navigate("/")} className="gap-1">
          <ArrowLeft className="h-4 w-4" />
          Thoát POS
        </Button>
        <span className="text-muted-foreground">Thu ngân: {user?.fullName}</span>
        <RealtimeClock />
      </header>
      <div className="flex-1 overflow-hidden">
        <Outlet />
      </div>
    </div>
  );
}
