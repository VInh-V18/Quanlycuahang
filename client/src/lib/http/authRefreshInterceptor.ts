import type { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from "axios";
import type { ApiFailure, ApiSuccess } from "@/types/api";

declare module "axios" {
  export interface InternalAxiosRequestConfig {
    _retry?: boolean;
  }
}

interface AuthRefreshOptions {
  client: AxiosInstance;
  getToken: () => string | null;
  setToken: (token: string | null) => void;
  refreshUrl: string;
}

/**
 * Interceptor 401→refresh dùng chung cho apiClient (tenant User, token trong Redux) và
 * platformAdminApiClient (Super Admin, token trong biến module) — trước đây cơ chế
 * isRefreshing/pendingQueue/so-sánh-stale-token trước khi refresh lặp lại gần như y hệt ở 2 nơi,
 * fix nào cũng phải viết và suy luận lại 2 lần (phát hiện khi rà soát). Vẫn giữ nơi lưu token
 * RIÊNG cho mỗi client (đúng thiết kế tách biệt "quyền toàn hệ thống" vs "quyền 1 cửa hàng") qua
 * getToken/setToken truyền vào — chỉ chia sẻ phần cơ chế retry/queue.
 */
export function registerAuthRefreshInterceptor({
  client,
  getToken,
  setToken,
  refreshUrl,
}: AuthRefreshOptions): void {
  let isRefreshing = false;
  let pendingQueue: Array<(token: string | null) => void> = [];

  function resolveQueue(token: string | null) {
    pendingQueue.forEach((resolve) => resolve(token));
    pendingQueue = [];
  }

  client.interceptors.response.use(
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
            resolve(client(originalRequest));
          });
        });
      }

      // Request này gửi đi với token CŨ và một lần refresh khác đã hoàn tất trong lúc nó đang bay
      // (ngoài cửa sổ isRefreshing) — chỉ cần thử lại với token hiện tại, KHÔNG gọi refresh lần
      // nữa: refresh token xoay vòng theo family, gọi lặp với cookie đã xoay có thể vô hiệu hóa cả
      // phiên (phát hiện khi rà soát).
      const currentToken = getToken();
      const sentAuthHeader = originalRequest.headers.get("Authorization");
      const sentToken =
        typeof sentAuthHeader === "string" ? sentAuthHeader.replace(/^Bearer /, "") : null;
      if (currentToken && sentToken && currentToken !== sentToken) {
        originalRequest._retry = true;
        originalRequest.headers.set("Authorization", `Bearer ${currentToken}`);
        return client(originalRequest);
      }

      originalRequest._retry = true;
      isRefreshing = true;
      try {
        const refreshResponse = await client.post<ApiSuccess<{ accessToken: string }>>(
          refreshUrl,
        );
        const newToken = refreshResponse.data.data.accessToken;
        setToken(newToken);
        resolveQueue(newToken);
        originalRequest.headers.set("Authorization", `Bearer ${newToken}`);
        return client(originalRequest);
      } catch (refreshError) {
        resolveQueue(null);
        setToken(null);
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    },
  );
}
