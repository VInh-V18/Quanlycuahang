import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface PurchaseOrderItemRequest {
  productId: number;
  quantity: number;
  unitPrice: number;
  batchCode?: string;
  expiryDate?: string;
}

export interface PurchaseOrderRequest {
  supplierId: number;
  branchId: number;
  items: PurchaseOrderItemRequest[];
  paidAmount: number;
  discountAmount: number;
}

export interface PurchaseOrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface PurchaseOrder {
  id: number;
  supplierId: number;
  supplierName: string;
  branchId: number;
  status: string;
  totalAmount: number;
  discountAmount: number;
  items: PurchaseOrderItem[];
  createdAt: string;
}

export async function listPurchaseOrders(
  branchId: number,
  page = 0,
): Promise<ApiSuccess<PurchaseOrder[]>> {
  const response = await apiClient.get<ApiSuccess<PurchaseOrder[]>>("/purchase-orders", {
    params: { branchId, page, size: 20 },
  });
  return response.data;
}

export async function getPurchaseOrder(id: number): Promise<PurchaseOrder> {
  const response = await apiClient.get<ApiSuccess<PurchaseOrder>>(`/purchase-orders/${id}`);
  return response.data.data;
}

export async function createPurchaseOrder(request: PurchaseOrderRequest): Promise<PurchaseOrder> {
  const response = await apiClient.post<ApiSuccess<PurchaseOrder>>("/purchase-orders", request);
  return response.data.data;
}
