import { apiClient } from "@/lib/http/apiClient";
import type { ApiSuccess } from "@/types/api";

export interface AuditLogItem {
  id: number;
  createdAt: string;
  userFullName: string | null;
  action: string;
  entityName: string | null;
  entityId: number | null;
  before: string | null;
  after: string | null;
}

export interface AuditLogParams {
  from?: string;
  to?: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function listAuditLogs(params: AuditLogParams): Promise<ApiSuccess<AuditLogItem[]>> {
  const response = await apiClient.get<ApiSuccess<AuditLogItem[]>>("/audit-logs", { params });
  return response.data;
}
