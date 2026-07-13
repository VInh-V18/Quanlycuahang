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

// updateRolePermissions da bi go bo: endpoint tenant PUT /roles/{id}/permissions khong con ton tai
// (Role la du lieu toan cuc, viec sua da chuyen han sang Super Admin o /platform-admin/roles).
