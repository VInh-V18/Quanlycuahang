import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface Supplier {
  id: number;
  name: string;
  phone: string | null;
  address: string | null;
  outstandingDebt: number;
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

export async function createSupplier(request: SupplierRequest): Promise<Supplier> {
  const response = await apiClient.post<ApiSuccess<Supplier>>("/suppliers", request);
  return response.data.data;
}
