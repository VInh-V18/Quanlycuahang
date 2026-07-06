import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface CustomerGroup {
  id: number;
  name: string;
}

export async function listCustomerGroups(): Promise<CustomerGroup[]> {
  const response = await apiClient.get<ApiSuccess<CustomerGroup[]>>("/customer-groups");
  return response.data.data;
}
