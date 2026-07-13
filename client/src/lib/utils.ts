import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Định dạng ngày/giờ dùng chung "vi-VN" — trước đây mỗi trang tự gọi
 * `new Date(x).toLocaleString("vi-VN")`/`toLocaleDateString("vi-VN", {...})` rải rác (12+ file),
 * tuỳ chọn định dạng đã bắt đầu lệch nhau giữa các nơi gọi (phát hiện khi rà soát).
 */
export function formatDateTime(value: string | number | Date): string {
  return new Date(value).toLocaleString("vi-VN");
}

export function formatDate(value: string | number | Date): string {
  return new Date(value).toLocaleDateString("vi-VN");
}
