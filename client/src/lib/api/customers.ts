import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface Customer {
  id: number;
  name: string;
  phone: string | null;
  address: string | null;
  email: string | null;
  customerGroupId: number | null;
  debtLimit: number;
}

export interface CustomerRequest {
  name: string;
  phone?: string | null;
  address?: string | null;
  email?: string | null;
  customerGroupId?: number | null;
  debtLimit?: number;
}

export async function searchCustomers(search: string): Promise<ApiSuccess<Customer[]>> {
  const response = await apiClient.get<ApiSuccess<Customer[]>>("/customers", {
    params: { search, size: 10 },
  });
  return response.data;
}

export async function createCustomer(request: CustomerRequest): Promise<Customer> {
  const response = await apiClient.post<ApiSuccess<Customer>>("/customers", request);
  return response.data.data;
}

export async function updateCustomer(id: number, request: CustomerRequest): Promise<Customer> {
  const response = await apiClient.put<ApiSuccess<Customer>>(`/customers/${id}`, request);
  return response.data.data;
}

export interface CustomerListItem {
  id: number;
  name: string;
  phone: string | null;
  address: string | null;
  customerGroupId: number | null;
  groupName: string | null;
  debtLimit: number;
  totalPurchased: number;
  orderCount: number;
  lastPurchaseAt: string | null;
  currentDebt: number;
}

export interface CustomerListParams {
  search?: string;
  customerGroupId?: number;
  hasDebt?: boolean;
  page?: number;
  size?: number;
}

export async function listCustomersWithStats(
  params: CustomerListParams,
): Promise<ApiSuccess<CustomerListItem[]>> {
  const response = await apiClient.get<ApiSuccess<CustomerListItem[]>>("/customers/list", {
    params,
  });
  return response.data;
}
