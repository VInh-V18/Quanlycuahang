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
