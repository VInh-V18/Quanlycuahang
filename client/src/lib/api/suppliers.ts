import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface Supplier {
  id: number;
  name: string;
  phone: string | null;
  address: string | null;
  outstandingDebt: number;
  totalPurchased: number;
  orderCount: number;
  lastPurchaseAt: string | null;
}

export interface SupplierRequest {
  name: string;
  phone?: string | null;
  address?: string | null;
}

export async function listSuppliers(): Promise<ApiSuccess<Supplier[]>> {
  const response = await apiClient.get<ApiSuccess<Supplier[]>>("/suppliers", {
    params: { size: 200 },
  });
  return response.data;
}

export interface SupplierListParams {
  search?: string;
  page?: number;
  size?: number;
}

export async function listSuppliersWithStats(
  params: SupplierListParams,
): Promise<ApiSuccess<Supplier[]>> {
  const response = await apiClient.get<ApiSuccess<Supplier[]>>("/suppliers/list", { params });
  return response.data;
}

export async function createSupplier(request: SupplierRequest): Promise<Supplier> {
  const response = await apiClient.post<ApiSuccess<Supplier>>("/suppliers", request);
  return response.data.data;
}

export async function updateSupplier(id: number, request: SupplierRequest): Promise<Supplier> {
  const response = await apiClient.put<ApiSuccess<Supplier>>(`/suppliers/${id}`, request);
  return response.data.data;
}
