import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface SettingsOverview {
  settings: Record<string, string>;
  systemInfo: Record<string, string>;
}

export interface Branch {
  id: number;
  name: string;
  address: string | null;
  phone: string | null;
  active: boolean;
}

export async function getSettings(): Promise<SettingsOverview> {
  const response = await apiClient.get<ApiSuccess<SettingsOverview>>("/settings");
  return response.data.data;
}

export async function updateSettings(updates: Record<string, string>): Promise<Record<string, string>> {
  const response = await apiClient.put<ApiSuccess<Record<string, string>>>("/settings", updates);
  return response.data.data;
}

export async function listBranches(): Promise<Branch[]> {
  const response = await apiClient.get<ApiSuccess<Branch[]>>("/branches");
  return response.data.data;
}

/** Chi nhanh nguoi dung hien tai duoc phep chuyen tren Topbar (owner/manager thay toan chuoi, cac
 * role con lai chi thay chi nhanh duoc gan) — khac voi listBranches() la danh sach toan he thong,
 * doi hoi quyen branch:view ma khong phai role nao cung co. */
export async function listMyBranches(): Promise<Branch[]> {
  const response = await apiClient.get<ApiSuccess<Branch[]>>("/branches/mine");
  return response.data.data;
}

export interface Branding {
  storeName: string;
  storeSlogan: string;
}

/** Ten/khau hieu cua hang hien thi cong khai (Sidebar, trang dang nhap) — endpoint public, goi
 * duoc ca khi chua dang nhap. */
export async function getBranding(): Promise<Branding> {
  const response = await apiClient.get<ApiSuccess<Branding>>("/settings/branding");
  return response.data.data;
}

export interface BranchUpdateRequest {
  name: string;
  address: string | null;
  phone: string | null;
}

export async function updateBranch(id: number, request: BranchUpdateRequest): Promise<Branch> {
  const response = await apiClient.put<ApiSuccess<Branch>>(`/branches/${id}`, request);
  return response.data.data;
}

export async function createBranch(request: BranchUpdateRequest): Promise<Branch> {
  const response = await apiClient.post<ApiSuccess<Branch>>("/branches", request);
  return response.data.data;
}

export async function deleteBranch(id: number): Promise<void> {
  await apiClient.delete(`/branches/${id}`);
}
