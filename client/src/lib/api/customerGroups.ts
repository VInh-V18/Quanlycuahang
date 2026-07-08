import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface CustomerGroup {
  id: number;
  name: string;
}

export interface CustomerGroupRequest {
  name: string;
}

export async function listCustomerGroups(): Promise<CustomerGroup[]> {
  const response = await apiClient.get<ApiSuccess<CustomerGroup[]>>("/customer-groups");
  return response.data.data;
}

export async function createCustomerGroup(request: CustomerGroupRequest): Promise<CustomerGroup> {
  const response = await apiClient.post<ApiSuccess<CustomerGroup>>("/customer-groups", request);
  return response.data.data;
}

export async function updateCustomerGroup(
  id: number,
  request: CustomerGroupRequest,
): Promise<CustomerGroup> {
  const response = await apiClient.put<ApiSuccess<CustomerGroup>>(`/customer-groups/${id}`, request);
  return response.data.data;
}

export async function deleteCustomerGroup(id: number): Promise<void> {
  await apiClient.delete(`/customer-groups/${id}`);
}
