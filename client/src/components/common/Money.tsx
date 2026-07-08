import { cn } from "@/lib/utils";

export interface MoneyProps {
  /** Số tiền VND — luôn số nguyên (NUMERIC(15,0) phía Backend, D5). */
  value: number;
  className?: string;
  /** Hiện dấu +/- (dùng cho tiền thừa/hoàn tiền). */
  showSign?: boolean;
}

const formatter = new Intl.NumberFormat("vi-VN");

/** Hiển thị tiền VND đồng nhất toàn hệ thống — luôn tabular-nums để căn cột đều trong bảng/hóa đơn. */
export function Money({ value, className, showSign = false }: MoneyProps) {
  const sign = showSign && value > 0 ? "+" : "";
  return (
    <span className={cn("tabular-nums", className)}>
      {sign}
      {formatter.format(value)}&nbsp;₫
    </span>
  );
}
