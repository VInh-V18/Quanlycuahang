import { isAxiosError } from "axios";
import type { ApiFailure } from "@/types/api";

/** Backend gắn header X-Correlation-Id cho MỌI response (Prompt #8, P2 quan sát — CorrelationIdFilter)
 * — đây chính là mã dùng để tra log/metric theo đúng 1 request. Chỉ hữu ích khi đính kèm cho lỗi
 * KHÔNG rõ nguyên nhân (500/mất kết nối) — lỗi nghiệp vụ thường (sai định dạng số điện thoại...) đã
 * có message rõ ràng, gắn thêm mã sẽ gây rối mà không giúp ích gì cho người dùng. */
export function getApiErrorRequestId(error: unknown): string | null {
  if (!isAxiosError(error)) {
    return null;
  }
  return error.response?.headers?.["x-correlation-id"] ?? null;
}

/** Backend đã trả message tiếng Việt sẵn theo D2 (`error.message`) — lấy trực tiếp. Với lỗi
 * VALIDATION_ERROR, `error.message` chỉ là câu chung ("Dữ liệu không hợp lệ") còn lý do cụ thể
 * (VD: sai định dạng số điện thoại) nằm trong `error.details` theo từng field — ưu tiên hiển thị
 * details khi có để người dùng biết chính xác cần sửa gì, chỉ fallback khi lỗi mạng/không parse
 * được response. */
export function getApiErrorMessage(error: unknown, fallback = "Đã xảy ra lỗi, vui lòng thử lại"): string {
  if (!isAxiosError<ApiFailure>(error)) {
    return fallback;
  }
  // 429 (giới hạn tần suất) có nhánh riêng — trước đây rơi chung vào fallback khi body không đọc
  // được, người dùng đăng nhập sai nhiều lần chỉ thấy "Đã xảy ra lỗi, vui lòng thử lại" và càng
  // bấm thử lại càng bị khóa lâu (phát hiện khi rà soát). Ưu tiên message từ Backend (đã nói rõ
  // thời gian chờ), chỉ dùng câu mặc định rate-limit khi thiếu body.
  if (error.response?.status === 429) {
    return (
      error.response.data?.error?.message ??
      "Bạn thao tác quá nhanh hoặc sai quá nhiều lần, vui lòng chờ ít phút rồi thử lại"
    );
  }
  const apiError = error.response?.data?.error;
  if (!apiError) {
    return withRequestIdSuffix(fallback, error);
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
  // Chỉ gắn mã tra cứu cho lỗi hệ thống thật (500 hoặc không có code nghiệp vụ rõ) — lỗi nghiệp vụ
  // (400/403/404 với message cụ thể) không cần, người dùng đã biết chính xác vấn đề là gì.
  if (apiError.code === "INTERNAL_ERROR" || (error.response?.status ?? 500) >= 500) {
    return withRequestIdSuffix(apiError.message ?? fallback, error);
  }
  return apiError.message ?? fallback;
}

function withRequestIdSuffix(message: string, error: unknown): string {
  const requestId = getApiErrorRequestId(error);
  return requestId ? `${message} (Mã lỗi: ${requestId} — cung cấp mã này khi báo hỗ trợ)` : message;
}

export function getApiErrorCode(error: unknown): string | null {
  if (isAxiosError<ApiFailure>(error)) {
    return error.response?.data?.error?.code ?? null;
  }
  return null;
}

/** 404 dùng chung cho 2 trường hợp không phân biệt được từ FE: bản ghi không tồn tại, HOẶC thuộc
 * tenant khác (Backend cố ý trả cùng 1 lỗi cho cả 2 để không lộ thông tin — xem
 * TenantAwareRepositoryImpl) — trang chi tiết nên hiển thị 1 thông điệp trung lập cho cả 2. */
export function isNotFoundError(error: unknown): boolean {
  return isAxiosError<ApiFailure>(error) && error.response?.status === 404;
}
