/** Khớp đúng shape ApiResponse<T> của Backend (D2, xem docs/conventions.md). */

export interface PageMeta {
  page: number;
  limit: number;
  total: number;
}

export interface ApiSuccess<T> {
  success: true;
  data: T;
  meta?: PageMeta;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

export interface ApiFailure {
  success: false;
  error: ApiErrorBody;
}

export type ApiResponse<T> = ApiSuccess<T> | ApiFailure;
