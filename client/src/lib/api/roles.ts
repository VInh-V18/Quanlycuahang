import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface Role {
  id: number;
  code: string;
  displayName: string;
  permissionCodes: string[];
}

export interface Permission {
  id: number;
  code: string;
  description: string;
}

export async function listRoles(): Promise<Role[]> {
  const response = await apiClient.get<ApiSuccess<Role[]>>("/roles");
  return response.data.data;
}

export async function listPermissions(): Promise<Permission[]> {
  const response = await apiClient.get<ApiSuccess<Permission[]>>("/permissions");
  return response.data.data;
}

export async function updateRolePermissions(
  roleId: number,
  permissionCodes: string[],
): Promise<Role> {
  const response = await apiClient.put<ApiSuccess<Role>>(`/roles/${roleId}/permissions`, {
    permissionCodes,
  });
  return response.data.data;
}
