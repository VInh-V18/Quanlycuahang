import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { ApiFailure, ApiSuccess } from "@/types/api";

declare module "axios" {
  export interface InternalAxiosRequestConfig {
    _retry?: boolean;
  }
}

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

let isRefreshing = false;
let pendingQueue: Array<(token: string | null) => void> = [];

function resolveQueue(token: string | null) {
  pendingQueue.forEach((resolve) => resolve(token));
  pendingQueue = [];
}

platformAdminApiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiFailure>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig | undefined;
    const isAuthEndpoint =
      originalRequest?.url?.includes("/auth/login") ||
      originalRequest?.url?.includes("/auth/refresh");

    if (
      error.response?.status !== 401 ||
      !originalRequest ||
      originalRequest._retry ||
      isAuthEndpoint
    ) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        pendingQueue.push((token) => {
          if (!token) {
            reject(error);
            return;
          }
          originalRequest._retry = true;
          originalRequest.headers.set("Authorization", `Bearer ${token}`);
          resolve(platformAdminApiClient(originalRequest));
        });
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;
    try {
      const refreshResponse = await platformAdminApiClient.post<ApiSuccess<{ accessToken: string }>>(
        "/auth/refresh",
      );
      const newToken = refreshResponse.data.data.accessToken;
      setPlatformAdminAccessToken(newToken);
      resolveQueue(newToken);
      originalRequest.headers.set("Authorization", `Bearer ${newToken}`);
      return platformAdminApiClient(originalRequest);
    } catch (refreshError) {
      resolveQueue(null);
      setPlatformAdminAccessToken(null);
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

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

export interface PlatformAdminRole {
  id: number;
  code: string;
  displayName: string;
}

export async function listPlatformAdminRoles(): Promise<PlatformAdminRole[]> {
  const response = await platformAdminApiClient.get<ApiSuccess<PlatformAdminRole[]>>("/roles");
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

export async function resetTenantUserPassword(
  tenantId: number,
  userId: number,
  newPassword: string,
): Promise<void> {
  await platformAdminApiClient.post(`/tenants/${tenantId}/users/${userId}/reset-password`, {
    newPassword,
  });
}
