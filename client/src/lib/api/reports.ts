import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface RevenueBucket {
  label: string;
  revenue: number;
  orderCount: number;
}

export interface GrossProfit {
  revenue: number;
  costOfGoodsSold: number;
  returnImpact: number;
  grossProfit: number;
}

export interface TopProduct {
  productId: number;
  productName: string;
  sku: string;
  quantitySold: number;
  revenue: number;
}

export interface TopCustomer {
  customerId: number;
  customerName: string;
  orderCount: number;
  revenue: number;
}

export interface InventoryValue {
  label: string;
  totalValue: number;
  totalQuantity: number;
}

export interface DebtAgingBucket {
  bucket: string;
  totalAmount: number;
  debtCount: number;
}

export interface EmployeePerformance {
  cashierName: string;
  orderCount: number;
  revenue: number;
  averageOrderValue: number;
}

export type RevenueGroupBy = "day" | "week" | "month" | "branch" | "cashier";

export interface ReportRangeParams {
  from: string;
  to: string;
  branchId?: number;
}

async function get<T>(path: string, params: object): Promise<T> {
  const response = await apiClient.get<ApiSuccess<T>>(path, { params });
  return response.data.data;
}

export function getRevenue(params: ReportRangeParams & { groupBy: RevenueGroupBy }) {
  return get<RevenueBucket[]>("/reports/revenue", params);
}

export function getGrossProfit(params: ReportRangeParams) {
  return get<GrossProfit>("/reports/gross-profit", params);
}

export function getTopProducts(params: ReportRangeParams & { limit?: number }) {
  return get<TopProduct[]>("/reports/top-products", params);
}

export function getTopCustomers(params: ReportRangeParams & { limit?: number }) {
  return get<TopCustomer[]>("/reports/top-customers", params);
}

export function getEmployeePerformance(params: ReportRangeParams) {
  return get<EmployeePerformance[]>("/reports/employee-performance", params);
}

export function getInventoryValue(params: { branchId?: number; groupBy: "branch" | "category" }) {
  return get<InventoryValue[]>("/reports/inventory-value", params);
}

export function getDebtAging(direction: "receivable" | "payable") {
  return get<DebtAgingBucket[]>("/reports/debt-aging", { direction });
}

/** Endpoint export yêu cầu header Authorization nên không thể dùng <a href> trực tiếp — tải qua
 * apiClient (đã có interceptor gắn token) rồi kích hoạt tải xuống bằng Blob URL tạm thời. */
export async function downloadReportExcel(
  path: string,
  params: object,
  fileName: string,
): Promise<void> {
  const response = await apiClient.get(`/reports/${path}/export`, {
    params,
    responseType: "blob",
  });
  const url = window.URL.createObjectURL(response.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
