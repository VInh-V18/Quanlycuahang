/** Định dạng ngày kiểu hóa đơn bán lẻ VN: "Ngày 29 tháng 06 năm 2026". */
export function formatInvoiceDate(iso: string): string {
  const d = new Date(iso);
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  return `Ngày ${day} tháng ${month} năm ${d.getFullYear()}`;
}

/** Định dạng thời điểm in: "29/06/2026 10:29". */
export function formatPrintedAt(iso: string): string {
  const d = new Date(iso);
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  return `${day}/${month}/${d.getFullYear()} ${hours}:${minutes}`;
}
