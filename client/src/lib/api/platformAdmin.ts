import axios from "axios";
import type { ApiSuccess } from "@/types/api";
import { registerAuthRefreshInterceptor } from "@/lib/http/authRefreshInterceptor";

/**
 * Client rieng cho Super Admin - CO CHU DINH tach biet hoan toan khoi apiClient chinh (khong dung
 * chung Redux auth state/access token voi tenant User, khong goi API /auth/* cua tenant) de tranh
 * moi nham lan giua "quyen van hanh toan he thong" va "quyen trong 1 cua hang cu the" (xem Backend
 * PlatformAdminJwtAuthenticationFilter). Token giu trong bien module (RAM), khong localStorage,
 * mat khi tai lai trang - giong nguyen tac D3 da ap dung cho tenant User.
 */
let accessToken: string | null = null;

export function getPlatformAdminAccessToken(): string | null {
  return accessToken;
}

export function setPlatformAdminAccessToken(token: string | null): void {
  accessToken = token;
}

export const platformAdminApiClient = axios.create({
  baseURL: "/api/v1/platform-admin",
  withCredentials: true,
});

platformAdminApiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.set("Authorization", `Bearer ${accessToken}`);
  }
  return config;
});

registerAuthRefreshInterceptor({
  client: platformAdminApiClient,
  getToken: getPlatformAdminAccessToken,
  setToken: setPlatformAdminAccessToken,
  refreshUrl: "/auth/refresh",
});

export interface PlatformAdminLoginPayload {
  username: string;
  password: string;
}

export async function platformAdminLogin(payload: PlatformAdminLoginPayload): Promise<string> {
  const response = await platformAdminApiClient.post<ApiSuccess<{ accessToken: string }>>(
    "/auth/login",
    payload,
  );
  const token = response.data.data.accessToken;
  setPlatformAdminAccessToken(token);
  return token;
}

export async function platformAdminLogout(): Promise<void> {
  try {
    await platformAdminApiClient.post("/auth/logout");
  } finally {
    setPlatformAdminAccessToken(null);
  }
}

export async function platformAdminChangePassword(
  oldPassword: string,
  newPassword: string,
): Promise<void> {
  await platformAdminApiClient.post("/auth/change-password", { oldPassword, newPassword });
}

export interface TenantDto {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
}

export async function listTenants(): Promise<TenantDto[]> {
  const response = await platformAdminApiClient.get<ApiSuccess<TenantDto[]>>("/tenants");
  return response.data.data;
}

export interface CreateTenantPayload {
  tenantName: string;
  branchName?: string;
  ownerUsername: string;
  ownerPassword: string;
  ownerFullName: string;
}

export async function createTenant(payload: CreateTenantPayload): Promise<TenantDto> {
  const response = await platformAdminApiClient.post<ApiSuccess<TenantDto>>("/tenants", payload);
  return response.data.data;
}

export async function setTenantActive(id: number, active: boolean): Promise<TenantDto> {
  const response = await platformAdminApiClient.put<ApiSuccess<TenantDto>>(
    `/tenants/${id}/active`,
    { active },
  );
  return response.data.data;
}

/** XÓA VĨNH VIỄN cửa hàng và TOÀN BỘ dữ liệu nghiệp vụ (đơn hàng, khách hàng, sản phẩm, công nợ,
 * hóa đơn...) — khác hẳn setTenantActive(false) (chỉ khóa, vẫn giữ dữ liệu). Không thể hoàn tác. */
export async function deleteTenant(id: number): Promise<void> {
  await platformAdminApiClient.delete(`/tenants/${id}`);
}

export interface PlatformAdminRole {
  id: number;
  code: string;
  displayName: string;
  permissionCodes: string[];
}

export async function listPlatformAdminRoles(): Promise<PlatformAdminRole[]> {
  const response = await platformAdminApiClient.get<ApiSuccess<PlatformAdminRole[]>>("/roles");
  return response.data.data;
}

export interface PlatformAdminPermission {
  id: number;
  code: string;
  description: string;
}

/** Danh sách toàn bộ permission cho ma trận phân quyền — endpoint riêng dưới /platform-admin vì
 * token Super Admin không dùng được endpoint /permissions của tenant (2 loại token từ chối lẫn
 * nhau có chủ đích). */
export async function listPlatformAdminPermissions(): Promise<PlatformAdminPermission[]> {
  const response =
    await platformAdminApiClient.get<ApiSuccess<PlatformAdminPermission[]>>("/roles/permissions");
  return response.data.data;
}

/** Sửa tập quyền của 1 vai trò — thay THẾ TOÀN BỘ tập quyền (full replace), ảnh hưởng MỌI cửa
 * hàng trên hệ thống vì Role là dữ liệu toàn cục. */
export async function updatePlatformAdminRolePermissions(
  roleId: number,
  permissionCodes: string[],
): Promise<PlatformAdminRole> {
  const response = await platformAdminApiClient.put<ApiSuccess<PlatformAdminRole>>(
    `/roles/${roleId}/permissions`,
    { permissionCodes },
  );
  return response.data.data;
}

export interface TenantUser {
  id: number;
  username: string;
  fullName: string;
  phone: string | null;
  active: boolean;
  roles: PlatformAdminRole[];
}

export interface TenantUserCreateRequest {
  username: string;
  password: string;
  fullName: string;
  phone?: string | null;
  roleIds: number[];
}

export interface TenantUserUpdateRequest {
  fullName: string;
  phone?: string | null;
  roleIds: number[];
  active: boolean;
}

export async function listTenantUsers(tenantId: number): Promise<TenantUser[]> {
  const response = await platformAdminApiClient.get<ApiSuccess<TenantUser[]>>(
    `/tenants/${tenantId}/users`,
  );
  return response.data.data;
}

export async function createTenantUser(
  tenantId: number,
  request: TenantUserCreateRequest,
): Promise<TenantUser> {
  const response = await platformAdminApiClient.post<ApiSuccess<TenantUser>>(
    `/tenants/${tenantId}/users`,
    request,
  );
  return response.data.data;
}

export async function updateTenantUser(
  tenantId: number,
  userId: number,
  request: TenantUserUpdateRequest,
): Promise<TenantUser> {
  const response = await platformAdminApiClient.put<ApiSuccess<TenantUser>>(
    `/tenants/${tenantId}/users/${userId}`,
    request,
  );
  return response.data.data;
}

export async function deactivateTenantUser(tenantId: number, userId: number): Promise<void> {
  await platformAdminApiClient.delete(`/tenants/${tenantId}/users/${userId}`);
}

/** XÓA VĨNH VIỄN tài khoản — khác hẳn deactivateTenantUser (chỉ khóa đăng nhập, giữ nguyên dữ
 * liệu). Backend từ chối (409/422) nếu tài khoản đã phát sinh hoạt động thật (đơn hàng, ca làm
 * việc...) — chỉ xóa được tài khoản "sạch", ngược lại phải dùng khóa thay vì xóa. */
export async function deleteTenantUser(tenantId: number, userId: number): Promise<void> {
  await platformAdminApiClient.delete(`/tenants/${tenantId}/users/${userId}/permanent`);
}

export async function resetTenantUserPassword(
  tenantId: number,
  userId: number,
  newPassword: string,
): Promise<void> {
  await platformAdminApiClient.post(`/tenants/${tenantId}/users/${userId}/reset-password`, {
    newPassword,
  });
}
