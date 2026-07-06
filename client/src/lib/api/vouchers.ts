import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface VoucherPreview {
  code: string;
  discountAmount: number;
}

export async function validateVoucher(code: string, subtotal: number): Promise<VoucherPreview> {
  const response = await apiClient.get<ApiSuccess<VoucherPreview>>("/vouchers/validate", {
    params: { code, subtotal },
  });
  return response.data.data;
}
