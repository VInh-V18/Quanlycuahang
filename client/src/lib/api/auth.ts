import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface LoginPayload {
  username: string;
  password: string;
}

export async function login(payload: LoginPayload): Promise<string> {
  const response = await apiClient.post<ApiSuccess<{ accessToken: string }>>(
    "/auth/login",
    payload,
  );
  return response.data.data.accessToken;
}

export async function logout(): Promise<void> {
  await apiClient.post("/auth/logout");
}
