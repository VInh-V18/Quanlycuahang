import { NavLink } from "react-router-dom";
import { ChevronsLeft, ChevronsRight } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { toggleSidebar } from "@/store/slices/uiSlice";
import { PermissionGate } from "@/components/common/PermissionGate";
import { navGroups } from "@/components/layout/nav-config";

export function Sidebar() {
  const collapsed = useAppSelector((state) => state.ui.sidebarCollapsed);
  const dispatch = useAppDispatch();

  return (
    <aside
      className={cn(
        "flex h-full flex-col bg-sidebar text-sidebar-foreground transition-all duration-200",
        collapsed ? "w-16" : "w-64",
      )}
    >
      <div className="flex h-14 shrink-0 items-center gap-3 border-b border-sidebar-border px-4">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground">
          FH
        </div>
        {!collapsed && (
          <div className="min-w-0">
            <div className="truncate text-sm font-bold leading-tight">FruitHouse</div>
            <div className="truncate text-xs leading-tight text-sidebar-muted">Chuỗi cửa hàng trái cây</div>
          </div>
        )}
      </div>

      <nav className="flex-1 space-y-4 overflow-y-auto px-2 py-3">
        {navGroups.map((group) => (
          <div key={group.label}>
            {!collapsed && (
              <div className="px-3 pb-1 text-[11px] font-semibold uppercase tracking-wider text-sidebar-muted">
                {group.label}
              </div>
            )}
            <div className="space-y-0.5">
              {group.items.map((item) => {
                const Icon = item.icon;
                const link = (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      cn(
                        "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                        isActive
                          ? "bg-sidebar-active text-sidebar-foreground"
                          : "text-sidebar-muted hover:bg-sidebar-border hover:text-sidebar-foreground",
                      )
                    }
                    title={collapsed ? item.label : undefined}
                  >
                    <Icon className="h-5 w-5 shrink-0" />
                    {!collapsed && <span className="truncate">{item.label}</span>}
                  </NavLink>
                );

                if (!item.perm) return link;
                return (
                  <PermissionGate key={item.to} perm={item.perm}>
                    {link}
                  </PermissionGate>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      <button
        onClick={() => dispatch(toggleSidebar())}
        className="flex items-center justify-center gap-2 border-t border-sidebar-border p-3 text-sm text-sidebar-muted hover:bg-sidebar-border hover:text-sidebar-foreground"
        aria-label={collapsed ? "Mở rộng menu" : "Thu gọn menu"}
      >
        {collapsed ? <ChevronsRight className="h-4 w-4" /> : <ChevronsLeft className="h-4 w-4" />}
        {!collapsed && "Thu gọn"}
      </button>
    </aside>
  );
}
