import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";
import type { RevenueBucket, TopProduct } from "@/lib/api/reports";

export interface RecentOrder {
  orderNumber: string;
  customerName: string;
  totalAmount: number;
  status: string;
}

export interface LowStockItem {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  stock: number;
  costPrice: number;
  minStock: number;
}

export interface DashboardSummary {
  todayRevenue: number;
  revenueChangePercent: number | null;
  todayOrderCount: number;
  orderCountDelta: number;
  averageOrderValue: number;
  grossProfitToday: number;
  grossProfitMarginPercent: number;
  returnCountToday: number;
  refundAmountToday: number;
  last7Days: RevenueBucket[];
  topProducts: TopProduct[];
  recentOrders: RecentOrder[];
  lowStock: LowStockItem[];
}

export async function getDashboardSummary(branchId?: number): Promise<DashboardSummary> {
  const response = await apiClient.get<ApiSuccess<DashboardSummary>>("/dashboard/summary", {
    params: { branchId },
  });
  return response.data.data;
}
