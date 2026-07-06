import { apiClient } from "@/lib/http/apiClient";
import { store } from "@/store";
import { setAccessToken } from "@/store/slices/authSlice";

/** Access token chỉ sống trong RAM (D3) nên mất khi refresh trang — thử "silent refresh"
 * 1 lần lúc khởi động bằng refresh token httpOnly cookie (nếu còn hạn) để không bắt
 * thu ngân đăng nhập lại chỉ vì lỡ F5 giữa ca. Thất bại (không có cookie/hết hạn) thì
 * coi như chưa đăng nhập, không phải lỗi. */
export async function bootstrapSession(): Promise<void> {
  try {
    const response = await apiClient.post<{ data: { accessToken: string } }>("/auth/refresh");
    store.dispatch(setAccessToken(response.data.data.accessToken));
  } catch {
    // Chưa đăng nhập hoặc refresh token đã hết hạn — im lặng, để RequireAuth điều hướng /login.
  }
}
