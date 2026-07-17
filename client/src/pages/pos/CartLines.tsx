import { memo } from "react";
import { Minus, Plus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Money } from "@/components/common/Money";
import { NumberInput } from "@/components/common/NumberInput";

const numberFormatter = new Intl.NumberFormat("vi-VN");

export interface CartLine {
  productId: number;
  name: string;
  sku: string;
  unit: string;
  /** Gia ban HIEU LUC — cho phep thu ngan sua truc tiep (VD: thuong luong voi khach). Khong bao
   * gio vuot qua catalogPrice (chi giam gia, khong tang) de tuong thich voi lineDiscountAmount
   * (>=0) da co san o Backend — xem lineDiscount() ben duoi. */
  unitPrice: number;
  /** Gia ban goc trong danh muc luc them vao gio — dung lam moc de tinh chiet khau khi sua gia va
   * de hien "gach gia goc" khi da sua. */
  catalogPrice: number;
  /** Gia von tham khao (binh quan gia quyen) — CHI HIEN, khong sua duoc o day (chi doi khi nhap
   * hang, xem ProductFormPage). */
  costPrice: number | null;
  vatRate: number;
  quantity: number;
  stock: number;
}

/** Chiet khau dong suy ra tu chenh lech gia ban hien tai voi gia goc — tinh lai moi lan (khong luu
 * rieng) de luon khop dung khi so luong doi sau khi da sua gia. */
export function lineDiscount(line: CartLine): number {
  const catalogPrice = line.catalogPrice ?? line.unitPrice;
  return Math.max(0, Math.round((catalogPrice - line.unitPrice) * line.quantity));
}

interface CartLinesProps {
  cart: CartLine[];
  onUpdatePrice: (productId: number, rawValue: number) => void;
  onUpdateQuantity: (productId: number, quantity: number) => void;
  onRemoveLine: (productId: number) => void;
}

/** Tach rieng khoi PosPage.tsx + boc React.memo (phat hien qua audit production readiness
 * 2026-07-17) — truoc day sua gio hang lam re-render lai CA luoi san pham ben canh du khong lien
 * quan, vi ca 2 khoi nam chung 1 component. Callback (onUpdatePrice/onUpdateQuantity/onRemoveLine)
 * la ham on dinh tu useState setter dang "functional update" (xem PosPage.tsx), khong doi tham
 * chieu moi lan render nen memo o day thuc su co tac dung. */
export const CartLines = memo(function CartLines({
  cart,
  onUpdatePrice,
  onUpdateQuantity,
  onRemoveLine,
}: CartLinesProps) {
  if (cart.length === 0) {
    return <p className="py-6 text-center text-sm text-muted-foreground">Giỏ hàng trống</p>;
  }

  return (
    <>
      {cart.map((line, index) => {
        const discount = lineDiscount(line);
        const isEdited = line.unitPrice !== line.catalogPrice;
        const belowCost = line.costPrice != null && line.unitPrice < line.costPrice;
        const lineTotal = Math.round(line.catalogPrice * line.quantity) - discount;
        return (
          <div key={line.productId} className="flex items-start justify-between gap-2 border-b pb-1.5">
            <div className="flex-1">
              <div className="text-sm font-medium">
                {index + 1}. {line.name}
              </div>
              <div className="flex flex-wrap items-center gap-x-1.5 gap-y-0.5 text-xs text-muted-foreground">
                <NumberInput
                  min={0}
                  value={line.unitPrice}
                  onValueChange={(v) => onUpdatePrice(line.productId, v ?? 0)}
                  className="h-6 w-[4.5rem] px-1.5 text-right text-xs"
                  title="Sửa giá bán dòng này"
                />
                {isEdited && (
                  <span className="line-through">{numberFormatter.format(line.catalogPrice)}</span>
                )}
                <span>
                  × {numberFormatter.format(line.quantity)} {line.unit}
                </span>
              </div>
              {line.costPrice != null && (
                <div className={`text-xs ${belowCost ? "font-medium text-destructive" : "text-muted-foreground"}`}>
                  Vốn: {numberFormatter.format(line.costPrice)}đ{belowCost && " · Bán dưới giá vốn"}
                </div>
              )}
              {/* line.stock được lưu từ lúc thêm vào giỏ nhưng trước đây không dùng để cảnh
                  báo gì — tăng số lượng vượt tồn không có tín hiệu nào trên UI, thu ngân chỉ
                  biết khi Backend từ chối lúc thanh toán (phát hiện khi rà soát). Chỉ cảnh
                  báo, không chặn: cửa hàng có thể đang bật "Cho phép bán âm kho". */}
              {line.quantity > line.stock && (
                <div className="text-xs font-medium text-destructive">
                  Vượt tồn kho (còn {numberFormatter.format(line.stock)} {line.unit})
                </div>
              )}
              <div className="mt-1 flex items-center gap-1">
                <Button
                  variant="outline"
                  size="icon"
                  className="h-6 w-6"
                  onClick={() => onUpdateQuantity(line.productId, line.quantity - 1)}
                >
                  <Minus className="h-3 w-3" />
                </Button>
                <NumberInput
                  // Go rong de nhap lai khong con xoa mat dong hang - NumberInput tu giu
                  // nguyen state cu trong luc dang go do/roi focus (xem NumberInput.tsx),
                  // khong can tu code guard rieng nhu truoc day nua (phat hien khi rieng soat).
                  value={line.quantity}
                  onValueChange={(v) => v != null && onUpdateQuantity(line.productId, v)}
                  className="h-6 w-14 text-center text-xs"
                />
                <Button
                  variant="outline"
                  size="icon"
                  className="h-6 w-6"
                  onClick={() => onUpdateQuantity(line.productId, line.quantity + 1)}
                >
                  <Plus className="h-3 w-3" />
                </Button>
              </div>
            </div>
            <div className="text-right">
              <div className="text-sm font-semibold">
                <Money value={lineTotal} />
              </div>
              {discount > 0 && (
                <div className="text-xs text-muted-foreground">
                  CK: −{numberFormatter.format(discount)}
                </div>
              )}
              <button
                onClick={() => onRemoveLine(line.productId)}
                className="mt-1 text-muted-foreground hover:text-destructive"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>
        );
      })}
    </>
  );
});
