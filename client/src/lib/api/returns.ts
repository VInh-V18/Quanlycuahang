import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface ReturnListItem {
  id: number;
  createdAt: string;
  orderNumber: string;
  customerName: string;
  customerPhone: string | null;
  totalRefund: number;
  refundMethod: string;
  itemCount: number;
  createdByName: string;
}

export interface ReturnListParams {
  branchId: number;
  from?: string;
  to?: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function listReturns(params: ReturnListParams): Promise<ApiSuccess<ReturnListItem[]>> {
  const response = await apiClient.get<ApiSuccess<ReturnListItem[]>>("/returns", { params });
  return response.data;
}

export interface ReturnItemRequest {
  orderItemId: number;
  quantity: number;
}

export interface ReturnRequest {
  orderId: number;
  items: ReturnItemRequest[];
  refundMethod: string;
}

export interface ReturnItemResult {
  orderItemId: number;
  productName: string;
  quantity: number;
  refundAmount: number;
}

export interface ReturnResult {
  id: number;
  orderId: number;
  totalRefund: number;
  refundMethod: string;
  items: ReturnItemResult[];
}

export async function createReturn(request: ReturnRequest): Promise<ReturnResult> {
  const response = await apiClient.post<ApiSuccess<ReturnResult>>("/returns", request);
  return response.data.data;
}
