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
