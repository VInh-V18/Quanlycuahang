import { apiClient } from "@/lib/http/apiClient";
import { downloadBlob, timestampedFileName } from "@/lib/download";
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

export interface InventoryListParams {
  search?: string;
  expiryThresholdDays?: number;
}

export async function listInventory(
  branchId: number,
  page = 0,
  size = 50,
  params: InventoryListParams = {},
): Promise<ApiSuccess<InventoryItem[]>> {
  const response = await apiClient.get<ApiSuccess<InventoryItem[]>>("/inventory", {
    params: { branchId, page, size, ...params },
  });
  return response.data;
}

export async function listLowStock(
  branchId: number,
  page = 0,
  size = 50,
  params: InventoryListParams = {},
): Promise<ApiSuccess<InventoryItem[]>> {
  const response = await apiClient.get<ApiSuccess<InventoryItem[]>>("/inventory/low-stock", {
    params: { branchId, page, size, ...params },
  });
  return response.data;
}

/** Xuat bang ton kho chi tiet (SKU, tồn, tối thiểu, lô/HSD, giá trị) — khác với báo cáo tổng giá
 * trị tồn kho gộp theo chi nhánh/danh mục ở trang Báo cáo. */
export async function exportInventory(branchId: number, lowStockOnly: boolean): Promise<void> {
  const response = await apiClient.get("/inventory/export", {
    params: { branchId, lowStockOnly },
    responseType: "blob",
  });
  downloadBlob(response.data as Blob, timestampedFileName("ton-kho.xlsx"));
}

export async function getInventoryTransactions(
  productId: number,
  branchId: number,
): Promise<InventoryTransaction[]> {
  const response = await apiClient.get<ApiSuccess<InventoryTransaction[]>>(
    `/inventory/products/${productId}/transactions`,
    { params: { branchId, size: 30 } },
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
