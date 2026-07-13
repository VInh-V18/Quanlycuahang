import { Outlet } from "react-router-dom";
import { FloatingAiButton } from "@/components/ai/FloatingAiButton";
import { Sidebar } from "@/components/layout/Sidebar";
import { Topbar } from "@/components/layout/Topbar";

/** Layout chính cho mọi trang trừ POS (docs/phase4/layout.md): Sidebar + Topbar. FloatingAiButton
 * (Prompt #12) tự ẩn hoàn toàn trên /pos dù dùng chung layout này (xem check pathname bên trong). */
export function MainLayout() {
  return (
    <div className="flex h-svh overflow-hidden print:h-auto print:overflow-visible">
      <div className="print:hidden">
        <Sidebar />
      </div>
      <div className="flex flex-1 flex-col overflow-hidden print:overflow-visible">
        <div className="print:hidden">
          <Topbar />
        </div>
        <main className="flex-1 overflow-y-auto p-6 print:overflow-visible print:p-0">
          <Outlet />
        </main>
      </div>
      <FloatingAiButton />
    </div>
  );
}
