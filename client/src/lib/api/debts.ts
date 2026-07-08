import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface DebtSummary {
  receivableTotal: number;
  receivableCount: number;
  payableTotal: number;
  payableCount: number;
  overdueReceivable: number;
}

export interface DebtPartnerAging {
  partnerId: number;
  partnerName: string;
  totalDebt: number;
  bucket0to7: number;
  bucket8to30: number;
  bucketOver30: number;
}

export interface DebtHistoryEvent {
  eventAt: string;
  label: string;
  referenceCode: string | null;
  amount: number;
}

export interface DebtPaymentRequest {
  direction: "receivable" | "payable";
  partnerId: number;
  amount: number;
  method: string;
  note?: string;
}

export async function getDebtSummary(): Promise<DebtSummary> {
  const response = await apiClient.get<ApiSuccess<DebtSummary>>("/debts/summary");
  return response.data.data;
}

export async function getDebtAgingByPartner(
  direction: "receivable" | "payable",
): Promise<DebtPartnerAging[]> {
  const response = await apiClient.get<ApiSuccess<DebtPartnerAging[]>>("/debts/by-partner", {
    params: { direction },
  });
  return response.data.data;
}

export async function getDebtHistory(
  partnerId: number,
  direction: "receivable" | "payable",
): Promise<DebtHistoryEvent[]> {
  const response = await apiClient.get<ApiSuccess<DebtHistoryEvent[]>>(
    `/debts/partners/${partnerId}/history`,
    { params: { direction } },
  );
  return response.data.data;
}

export async function recordDebtPayment(request: DebtPaymentRequest): Promise<void> {
  await apiClient.post("/debts/payments", request);
}
