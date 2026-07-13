import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface ReconciliationFinding {
  id: number;
  checkType: string;
  severity: "critical" | "high" | "medium" | "low";
  entityType: string | null;
  entityId: number | null;
  details: string | null;
  status: "open" | "acknowledged" | "resolved";
}

export interface ReconciliationRun {
  id: number;
  triggerType: "manual" | "scheduled";
  status: "running" | "completed" | "failed";
  findingsCount: number;
  durationMs: number | null;
  startedAt: string;
  finishedAt: string | null;
  findings?: ReconciliationFinding[];
}

export async function getOpenReconciliationFindingsCount(): Promise<number> {
  const response = await apiClient.get<ApiSuccess<number>>("/admin/reconciliation/open-count");
  return response.data.data;
}

export async function runReconciliation(): Promise<ReconciliationRun> {
  const response = await apiClient.post<ApiSuccess<ReconciliationRun>>("/admin/reconciliation/run");
  return response.data.data;
}
