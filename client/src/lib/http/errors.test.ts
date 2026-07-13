import { AxiosError, AxiosHeaders } from "axios";
import { describe, expect, it } from "vitest";
import { getApiErrorMessage, getApiErrorRequestId } from "@/lib/http/errors";
import type { ApiFailure } from "@/types/api";

function apiError(
  status: number,
  body: ApiFailure,
  responseHeaders: Record<string, string> = {},
): AxiosError<ApiFailure> {
  const error = new AxiosError<ApiFailure>("Request failed");
  error.response = {
    status,
    statusText: "",
    headers: new AxiosHeaders(responseHeaders),
    config: { headers: new AxiosHeaders() } as never,
    data: body,
  };
  return error;
}

describe("getApiErrorRequestId", () => {
  it("đọc header x-correlation-id (Prompt #8 - CorrelationIdFilter) từ response", () => {
    const error = apiError(
      500,
      { success: false, error: { code: "INTERNAL_ERROR", message: "Lỗi hệ thống" } },
      { "x-correlation-id": "abc-123" },
    );
    expect(getApiErrorRequestId(error)).toBe("abc-123");
  });

  it("trả về null khi không phải lỗi Axios", () => {
    expect(getApiErrorRequestId(new Error("not axios"))).toBeNull();
  });
});

describe("getApiErrorMessage", () => {
  it("gắn mã tra cứu cho lỗi 500/INTERNAL_ERROR để người dùng báo hỗ trợ", () => {
    const error = apiError(
      500,
      { success: false, error: { code: "INTERNAL_ERROR", message: "Đã xảy ra lỗi hệ thống" } },
      { "x-correlation-id": "req-500" },
    );
    expect(getApiErrorMessage(error)).toBe("Đã xảy ra lỗi hệ thống (Mã lỗi: req-500 — cung cấp mã này khi báo hỗ trợ)");
  });

  it("KHÔNG gắn mã tra cứu cho lỗi nghiệp vụ thường (400) - message đã đủ rõ", () => {
    const error = apiError(
      400,
      { success: false, error: { code: "CATEGORY_HAS_PRODUCTS", message: "Danh mục còn sản phẩm" } },
      { "x-correlation-id": "req-400" },
    );
    expect(getApiErrorMessage(error)).toBe("Danh mục còn sản phẩm");
  });

  it("gắn mã tra cứu cho fallback khi mất kết nối/không đọc được response", () => {
    const error = new AxiosError("Network Error");
    error.response = {
      status: 0,
      statusText: "",
      headers: new AxiosHeaders({ "x-correlation-id": "req-network" }),
      config: { headers: new AxiosHeaders() } as never,
      data: undefined,
    };
    expect(getApiErrorMessage(error, "Mất kết nối")).toBe(
      "Mất kết nối (Mã lỗi: req-network — cung cấp mã này khi báo hỗ trợ)",
    );
  });

  it("vẫn ưu tiên message rate-limit (429) như trước, không đổi hành vi cũ", () => {
    const error = apiError(429, {
      success: false,
      error: { code: "RATE_LIMITED", message: "Chờ 5 phút" },
    });
    expect(getApiErrorMessage(error)).toBe("Chờ 5 phút");
  });
});
