import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { store } from "@/store";
import { clearCredentials, setAccessToken } from "@/store/slices/authSlice";
import type { ApiFailure } from "@/types/api";

declare module "axios" {
  export interface InternalAxiosRequestConfig {
    _retry?: boolean;
  }
}

/** access token giữ ở Redux (RAM, không localStorage — D3); refresh token nằm trong
 * httpOnly cookie do Backend set, gửi tự động qua withCredentials. */
export const apiClient = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
});

apiClient.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

let isRefreshing = false;
let pendingQueue: Array<(token: string | null) => void> = [];

function resolveQueue(token: string | null) {
  pendingQueue.forEach((resolve) => resolve(token));
  pendingQueue = [];
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiFailure>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig | undefined;
    const isAuthEndpoint =
      originalRequest?.url?.includes("/auth/login") ||
      originalRequest?.url?.includes("/auth/refresh");

    if (error.response?.status !== 401 || !originalRequest || originalRequest._retry || isAuthEndpoint) {
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
          resolve(apiClient(originalRequest));
        });
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;
    try {
      const refreshResponse = await apiClient.post<{ data: { accessToken: string } }>(
        "/auth/refresh",
      );
      const newToken = refreshResponse.data.data.accessToken;
      store.dispatch(setAccessToken(newToken));
      resolveQueue(newToken);
      originalRequest.headers.set("Authorization", `Bearer ${newToken}`);
      return apiClient(originalRequest);
    } catch (refreshError) {
      resolveQueue(null);
      store.dispatch(clearCredentials());
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);
