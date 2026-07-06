import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface InventoryItem {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  stock: number;
  costPrice: number;
  minStock: number;
  nearestBatchCode: string | null;
  nearestExpiryDate: string | null;
}

export interface InventoryTransaction {
  id: number;
  type: string;
  quantity: number;
  unitCost: number;
  referenceType: string | null;
  referenceId: number | null;
  note: string | null;
  createdAt: string;
}

export async function listInventory(
  branchId: number,
  page = 0,
  size = 50,
): Promise<ApiSuccess<InventoryItem[]>> {
  const response = await apiClient.get<ApiSuccess<InventoryItem[]>>("/inventory", {
    params: { branchId, page, size },
  });
  return response.data;
}

export async function listLowStock(
  branchId: number,
  page = 0,
  size = 50,
): Promise<ApiSuccess<InventoryItem[]>> {
  const response = await apiClient.get<ApiSuccess<InventoryItem[]>>("/inventory/low-stock", {
    params: { branchId, page, size },
  });
  return response.data;
}

export async function getInventoryTransactions(
  productId: number,
): Promise<InventoryTransaction[]> {
  const response = await apiClient.get<ApiSuccess<InventoryTransaction[]>>(
    `/inventory/products/${productId}/transactions`,
    { params: { size: 30 } },
  );
  return response.data.data;
}

export const TRANSACTION_TYPE_LABELS: Record<string, string> = {
  sale: "Bán hàng",
  purchase: "Nhập NCC",
  customer_return: "Khách trả",
  stock_take: "Kiểm kê",
  cancel: "Hủy đơn",
};
