import { isAxiosError } from "axios";
import type { ApiFailure } from "@/types/api";

/** Backend đã trả message tiếng Việt sẵn theo D2 (`error.message`) — lấy trực tiếp. Với lỗi
 * VALIDATION_ERROR, `error.message` chỉ là câu chung ("Dữ liệu không hợp lệ") còn lý do cụ thể
 * (VD: sai định dạng số điện thoại) nằm trong `error.details` theo từng field — ưu tiên hiển thị
 * details khi có để người dùng biết chính xác cần sửa gì, chỉ fallback khi lỗi mạng/không parse
 * được response. */
export function getApiErrorMessage(error: unknown, fallback = "Đã xảy ra lỗi, vui lòng thử lại"): string {
  if (!isAxiosError<ApiFailure>(error)) {
    return fallback;
  }
  const apiError = error.response?.data?.error;
  if (!apiError) {
    return fallback;
  }
  const details = apiError.details;
  if (details) {
    const fieldMessages = Object.values(details).filter(
      (value): value is string => typeof value === "string" && value.trim().length > 0,
    );
    if (fieldMessages.length > 0) {
      return fieldMessages.join("; ");
    }
  }
  return apiError.message ?? fallback;
}

export function getApiErrorCode(error: unknown): string | null {
  if (isAxiosError<ApiFailure>(error)) {
    return error.response?.data?.error?.code ?? null;
  }
  return null;
}
