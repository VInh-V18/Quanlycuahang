import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface ParkedOrder {
  id: number;
  branchId: number;
  cartSnapshot: string;
  note: string | null;
  parkedAt: string;
}

export async function listParkedOrders(branchId: number): Promise<ParkedOrder[]> {
  const response = await apiClient.get<ApiSuccess<ParkedOrder[]>>("/parked-orders", {
    params: { branchId },
  });
  return response.data.data;
}

export async function parkOrder(
  branchId: number,
  cartSnapshot: string,
  note?: string,
): Promise<ParkedOrder> {
  const response = await apiClient.post<ApiSuccess<ParkedOrder>>("/parked-orders", {
    branchId,
    cartSnapshot,
    note,
  });
  return response.data.data;
}

export async function resumeParkedOrder(id: number): Promise<ParkedOrder> {
  const response = await apiClient.post<ApiSuccess<ParkedOrder>>(`/parked-orders/${id}/resume`);
  return response.data.data;
}
