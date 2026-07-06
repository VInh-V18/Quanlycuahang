import type { LucideIcon } from "lucide-react";
import {
  BarChart3,
  Boxes,
  CircleDollarSign,
  ClipboardCheck,
  FileText,
  LayoutDashboard,
  Package,
  PackagePlus,
  Receipt,
  Settings,
  ShoppingCart,
  Truck,
  Undo2,
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

export interface NavGroup {
  label: string;
  items: NavItem[];
}

/** Danh sách điều hướng Sidebar chính, nhóm theo mockup FruitHouse. */
export const navGroups: NavGroup[] = [
  {
    label: "Hoạt động",
    items: [
      { label: "Tổng quan", to: "/", icon: LayoutDashboard },
      { label: "Bán hàng (POS)", to: "/pos", icon: ShoppingCart, perm: ["order:create"] },
      { label: "Đơn hàng", to: "/orders", icon: Receipt, perm: ["order:view"] },
      { label: "Trả hàng", to: "/returns", icon: Undo2, perm: ["return:view"] },
    ],
  },
  {
    label: "Hàng hóa",
    items: [
      { label: "Sản phẩm", to: "/products", icon: Package, perm: ["product:view"] },
      { label: "Nhập kho", to: "/purchase-orders", icon: PackagePlus, perm: ["purchase-order:view"] },
      { label: "Tồn kho", to: "/inventory", icon: Boxes, perm: ["inventory:view"] },
      { label: "Kiểm kê", to: "/stock-takes", icon: ClipboardCheck, perm: ["stock-take:view"] },
    ],
  },
  {
    label: "Đối tác",
    items: [
      { label: "Khách hàng", to: "/customers", icon: Users, perm: ["customer:view"] },
      { label: "Nhà cung cấp", to: "/suppliers", icon: Truck, perm: ["supplier:view"] },
      { label: "Công nợ", to: "/debts", icon: Wallet, perm: ["debt:view"] },
    ],
  },
  {
    label: "Vận hành",
    items: [
      { label: "Hóa đơn", to: "/invoices", icon: FileText, perm: ["invoice:view"] },
      { label: "Ca & két tiền", to: "/shifts", icon: CircleDollarSign, perm: ["shift:view"] },
      { label: "Báo cáo", to: "/reports", icon: BarChart3, perm: ["report:revenue", "report:gross-profit"] },
      { label: "Nhân viên & phân quyền", to: "/employees", icon: UserCog, perm: ["employee:view"] },
      { label: "Cài đặt", to: "/settings", icon: Settings, perm: ["settings:view"] },
    ],
  },
];
