import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

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
