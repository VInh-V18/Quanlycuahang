import { useParams } from "react-router-dom";
import { InvoiceViewer } from "@/components/invoice/InvoiceViewer";

/** In lại hóa đơn (K80/A4) — Gate Phase 9: "in thử K80 đúng khổ, số liệu khớp đơn gốc từng đồng".
 * Backend chỉ trả JSON, toàn bộ layout in dựng ở FE (B3). */
export function InvoicePrintPage() {
  const { id } = useParams<{ id: string }>();
  if (!id) return null;
  return <InvoiceViewer invoiceId={id} />;
}
