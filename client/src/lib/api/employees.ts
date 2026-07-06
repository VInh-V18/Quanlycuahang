import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface RoleSummary {
  id: number;
  code: string;
  displayName: string;
}

export interface Employee {
  id: number;
  username: string;
  fullName: string;
  phone: string | null;
  active: boolean;
  roles: RoleSummary[];
}

export interface EmployeeCreateRequest {
  username: string;
  password: string;
  fullName: string;
  phone?: string | null;
  roleIds: number[];
}

export interface EmployeeUpdateRequest {
  fullName: string;
  phone?: string | null;
  roleIds: number[];
  active: boolean;
}

export async function listEmployees(): Promise<Employee[]> {
  const response = await apiClient.get<ApiSuccess<Employee[]>>("/employees");
  return response.data.data;
}

export async function createEmployee(request: EmployeeCreateRequest): Promise<Employee> {
  const response = await apiClient.post<ApiSuccess<Employee>>("/employees", request);
  return response.data.data;
}

export async function updateEmployee(
  id: number,
  request: EmployeeUpdateRequest,
): Promise<Employee> {
  const response = await apiClient.put<ApiSuccess<Employee>>(`/employees/${id}`, request);
  return response.data.data;
}

export async function deactivateEmployee(id: number): Promise<void> {
  await apiClient.delete(`/employees/${id}`);
}
