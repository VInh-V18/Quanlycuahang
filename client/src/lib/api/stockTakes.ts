import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface StockTakeItem {
  id: number;
  productId: number;
  productName: string;
  expectedQty: number;
  actualQty: number | null;
  reason: string | null;
}

export interface StockTake {
  id: number;
  branchId: number;
  status: "draft" | "approved";
  createdAt: string;
  approvedAt: string | null;
  items: StockTakeItem[];
}

export async function listStockTakes(
  branchId: number,
  page = 0,
): Promise<ApiSuccess<StockTake[]>> {
  const response = await apiClient.get<ApiSuccess<StockTake[]>>("/stock-takes", {
    params: { branchId, page, size: 20 },
  });
  return response.data;
}

export async function getStockTake(id: number): Promise<StockTake> {
  const response = await apiClient.get<ApiSuccess<StockTake>>(`/stock-takes/${id}`);
  return response.data.data;
}

export async function createStockTake(branchId: number): Promise<StockTake> {
  const response = await apiClient.post<ApiSuccess<StockTake>>("/stock-takes", { branchId });
  return response.data.data;
}

export interface ItemCount {
  stockTakeItemId: number;
  actualQty: number;
  reason?: string;
}

export async function submitCounts(id: number, items: ItemCount[]): Promise<StockTake> {
  const response = await apiClient.put<ApiSuccess<StockTake>>(`/stock-takes/${id}/counts`, {
    items,
  });
  return response.data.data;
}

export async function approveStockTake(id: number): Promise<StockTake> {
  const response = await apiClient.post<ApiSuccess<StockTake>>(`/stock-takes/${id}/approve`);
  return response.data.data;
}

export const DISCREPANCY_REASONS = [
  "Dập nát / hư hỏng",
  "Quá hạn hủy bỏ",
  "Sót phiếu nhập",
  "Sai số lúc nhập",
  "Khác",
];
