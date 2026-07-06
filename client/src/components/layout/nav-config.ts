import type { LucideIcon } from "lucide-react";
import {
  BarChart3,
  Boxes,
  ClipboardList,
  LayoutDashboard,
  Package,
  Receipt,
  Settings,
  ShoppingCart,
  Truck,
  Users,
  UserCog,
  Wallet,
} from "lucide-react";

export interface NavItem {
  label: string;
  to: string;
  icon: LucideIcon;
  /** Chỉ hiển thị nếu user có ít nhất 1 trong các quyền này (layout.md: mỗi mục cần ≥1 quyền view). */
  perm?: string[];
}

/** Danh sách điều hướng Sidebar chính — đúng thứ tự docs/phase4/layout.md. */
export const navItems: NavItem[] = [
  { label: "Dashboard", to: "/", icon: LayoutDashboard },
  { label: "Bán hàng (POS)", to: "/pos", icon: ShoppingCart, perm: ["order:create"] },
  { label: "Sản phẩm", to: "/products", icon: Package, perm: ["product:view"] },
  { label: "Kho", to: "/inventory", icon: Boxes, perm: ["inventory:view", "purchase-order:view", "stock-take:view"] },
  { label: "Đơn hàng / Trả hàng", to: "/orders", icon: Receipt, perm: ["order:view", "return:view"] },
  { label: "Khách hàng", to: "/customers", icon: Users, perm: ["customer:view"] },
  { label: "Nhà cung cấp", to: "/suppliers", icon: Truck, perm: ["supplier:view"] },
  { label: "Công nợ", to: "/debts", icon: Wallet, perm: ["debt:view"] },
  { label: "Ca & két", to: "/shifts", icon: ClipboardList, perm: ["shift:view"] },
  { label: "Báo cáo", to: "/reports", icon: BarChart3, perm: ["report:revenue", "report:gross-profit"] },
  { label: "Nhân viên & phân quyền", to: "/employees", icon: UserCog, perm: ["employee:view"] },
  { label: "Cài đặt", to: "/settings", icon: Settings, perm: ["settings:view"] },
];
