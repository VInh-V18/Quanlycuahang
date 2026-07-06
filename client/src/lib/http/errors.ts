import { isAxiosError } from "axios";
import type { ApiFailure } from "@/types/api";

/** Backend đã trả message tiếng Việt sẵn theo D2 (`error.message`) — lấy trực tiếp,
 * chỉ fallback khi lỗi mạng/không parse được response. */
export function getApiErrorMessage(error: unknown, fallback = "Đã xảy ra lỗi, vui lòng thử lại"): string {
  if (isAxiosError<ApiFailure>(error)) {
    return error.response?.data?.error?.message ?? fallback;
  }
  return fallback;
}

export function getApiErrorCode(error: unknown): string | null {
  if (isAxiosError<ApiFailure>(error)) {
    return error.response?.data?.error?.code ?? null;
  }
  return null;
}
