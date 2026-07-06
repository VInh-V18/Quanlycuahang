import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface CashTransaction {
  id: number;
  type: "cash_in" | "cash_out";
  amount: number;
  note: string | null;
  createdByName: string | null;
  createdAt: string;
}

export interface ShiftSummary {
  id: number;
  branchName: string;
  cashierName: string;
  openingCash: number;
  actualCash: number | null;
  discrepancy: number | null;
  note: string | null;
  status: "open" | "closed";
  openedAt: string;
  closedAt: string | null;
}

export interface ShiftDetail extends ShiftSummary {
  cashSalesTotal: number;
  bankTransferSalesTotal: number;
  cardSalesTotal: number;
  cashRefundTotal: number;
  cashInTotal: number;
  cashOutTotal: number;
  expectedCash: number;
  orderCount: number;
  cashTransactions: CashTransaction[];
}

export async function openShift(openingCash: number, note?: string): Promise<ShiftDetail> {
  const response = await apiClient.post<ApiSuccess<ShiftDetail>>("/shifts/open", {
    openingCash,
    note,
  });
  return response.data.data;
}

export async function getCurrentShift(): Promise<ShiftDetail | null> {
  const response = await apiClient.get<ApiSuccess<ShiftDetail | null>>("/shifts/current");
  return response.data.data ?? null;
}

export async function closeShift(
  id: number,
  actualCash: number,
  note?: string,
): Promise<ShiftDetail> {
  const response = await apiClient.post<ApiSuccess<ShiftDetail>>(`/shifts/${id}/close`, {
    actualCash,
    note,
  });
  return response.data.data;
}

export interface ShiftHistoryParams {
  status?: string;
  page?: number;
  size?: number;
}

export async function listShiftHistory(
  params: ShiftHistoryParams,
): Promise<ApiSuccess<ShiftSummary[]>> {
  const response = await apiClient.get<ApiSuccess<ShiftSummary[]>>("/shifts", { params });
  return response.data;
}

export async function getShiftDetail(id: number): Promise<ShiftDetail> {
  const response = await apiClient.get<ApiSuccess<ShiftDetail>>(`/shifts/${id}`);
  return response.data.data;
}

export async function addCashTransaction(
  shiftId: number,
  type: "cash_in" | "cash_out",
  amount: number,
  note?: string,
): Promise<CashTransaction> {
  const response = await apiClient.post<ApiSuccess<CashTransaction>>(
    `/shifts/${shiftId}/cash-transactions`,
    { type, amount, note },
  );
  return response.data.data;
}
