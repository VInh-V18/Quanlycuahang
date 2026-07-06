export type OrderStatusTone = "success" | "warning" | "destructive" | "info" | "muted";

export interface OrderStatusMeta {
  label: string;
  tone: OrderStatusTone;
}

/** "debt" khong phai 1 gia tri that cua orders.status — day la co dinh danh dau don co cong no
 * con treo (xem DashboardService.statusLabel phia Backend), uu tien hien thi truoc trang thai goc. */
const STATUS_MAP: Record<string, OrderStatusMeta> = {
  completed: { label: "Hoàn thành", tone: "success" },
  debt: { label: "Ghi nợ", tone: "warning" },
  partially_returned: { label: "Trả một phần", tone: "muted" },
  fully_returned: { label: "Đã trả hết", tone: "muted" },
  cancelled: { label: "Đã hủy", tone: "destructive" },
  draft: { label: "Nháp", tone: "info" },
};

export function orderStatusMeta(status: string): OrderStatusMeta {
  return STATUS_MAP[status] ?? { label: status, tone: "muted" };
}
