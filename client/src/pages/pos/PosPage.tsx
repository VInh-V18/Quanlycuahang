import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Money } from "@/components/common/Money";

/** Khung layout POS (Phase 5 — chỉ dựng khung 2 cột đúng docs/phase4/pos-design.md).
 * Logic giỏ hàng/thanh toán thật (OrderPricingService, quét barcode, phím tắt F1–F9...)
 * thuộc phạm vi xây dựng tính năng bán hàng phía FE — ngoài phạm vi "Frontend Foundation". */
export function PosPage() {
  return (
    <div className="grid h-full grid-cols-1 md:grid-cols-[60%_40%]">
      <div className="flex flex-col gap-4 overflow-y-auto border-r p-4">
        <div className="relative">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input autoFocus placeholder="Quét mã vạch hoặc tìm sản phẩm (F2)" className="pl-8" />
        </div>
        <div className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
          Lưới sản phẩm sẽ hiển thị ở đây (module bán hàng FE — ngoài phạm vi Phase 5)
        </div>
      </div>
      <div className="flex flex-col gap-4 p-4">
        <div className="flex-1 text-sm text-muted-foreground">Giỏ hàng trống</div>
        <div className="space-y-1 border-t pt-4">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Tổng hàng</span>
            <Money value={0} />
          </div>
          <div className="flex justify-between text-lg font-semibold">
            <span>Tổng thanh toán</span>
            <Money value={0} />
          </div>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" className="flex-1">
            Treo đơn
          </Button>
          <Button className="flex-1">Thanh toán (F9)</Button>
        </div>
      </div>
    </div>
  );
}
