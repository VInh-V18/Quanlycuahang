import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface OrderListItem {
  id: number;
  orderNumber: string;
  createdAt: string;
  status: string;
  totalAmount: number;
  customerName: string;
  customerPhone: string | null;
  cashierName: string;
  hasDebt: boolean;
  paymentMethods: string[];
}

export interface OrderListParams {
  branchId: number;
  from?: string;
  to?: string;
  status?: string;
  cashierId?: number;
  search?: string;
  page?: number;
  size?: number;
}

export async function listOrders(params: OrderListParams): Promise<ApiSuccess<OrderListItem[]>> {
  const response = await apiClient.get<ApiSuccess<OrderListItem[]>>("/orders", { params });
  return response.data;
}

export async function exportOrders(params: OrderListParams): Promise<void> {
  const response = await apiClient.get("/orders/export", { params, responseType: "blob" });
  const url = window.URL.createObjectURL(response.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "don-hang.xlsx";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  discountAmount: number;
  vatAmount: number;
  lineTotal: number;
  returnedQuantity: number;
}

export interface OrderDetail {
  id: number;
  orderNumber: string;
  status: string;
  branchId: number;
  customerId: number | null;
  subtotalAmount: number;
  discountAmount: number;
  vatAmount: number;
  totalAmount: number;
  items: OrderItem[];
  createdAt: string;
  invoiceId: number | null;
  invoiceNumber: string | null;
}

export async function getOrder(id: number): Promise<OrderDetail> {
  const response = await apiClient.get<ApiSuccess<OrderDetail>>(`/orders/${id}`);
  return response.data.data;
}

export interface OrderCreateLine {
  productId: number;
  quantity: number;
  lineDiscountAmount: number;
}

export interface OrderCreatePayment {
  method: string;
  amount: number;
}

export interface OrderCreateRequest {
  branchId: number;
  customerId?: number;
  voucherCode?: string;
  orderDiscountAmount: number;
  cashReceived?: number;
  expectedTotalAmount: number;
  lines: OrderCreateLine[];
  payments: OrderCreatePayment[];
}

export async function createOrder(
  request: OrderCreateRequest,
  idempotencyKey: string,
): Promise<OrderDetail> {
  const response = await apiClient.post<ApiSuccess<OrderDetail>>("/orders", request, {
    headers: { "Idempotency-Key": idempotencyKey },
  });
  return response.data.data;
}
