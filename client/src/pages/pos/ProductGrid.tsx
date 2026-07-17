import { memo } from "react";
import { Money } from "@/components/common/Money";
import type { Product } from "@/lib/api/products";
import { categoryEmoji } from "@/lib/pos/categoryEmoji";

const numberFormatter = new Intl.NumberFormat("vi-VN");

interface ProductGridProps {
  products: Product[];
  onAddProduct: (product: Product) => void;
}

/** Tach rieng khoi PosPage.tsx + boc React.memo (phat hien qua audit production readiness
 * 2026-07-17) — truoc day go tim kiem/sua gio hang lam re-render lai CA luoi san pham (toi da 60
 * item) du chi 1 phan thay doi, vi ca 2 khoi nam chung 1 component voi state search/cart. */
export const ProductGrid = memo(function ProductGrid({ products, onAddProduct }: ProductGridProps) {
  return (
    <div className="flex flex-col divide-y rounded-lg border">
      {products.map((product) => {
        const outOfStock = (product.stock ?? 0) <= 0;
        return (
          <button
            key={product.id}
            type="button"
            disabled={outOfStock}
            onClick={() => onAddProduct(product)}
            className="flex items-center gap-3 bg-card px-3 py-2 text-left transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
          >
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-accent text-lg">
              {categoryEmoji(product.categoryName)}
            </div>
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-medium leading-tight">{product.name}</div>
              <div className={`text-xs ${outOfStock ? "text-destructive" : "text-muted-foreground"}`}>
                {outOfStock ? "Hết hàng" : `Tồn ${numberFormatter.format(product.stock ?? 0)}`}
              </div>
            </div>
            <div className="shrink-0 text-sm font-semibold text-primary">
              <Money value={product.sellPrice} />/{product.unit}
            </div>
          </button>
        );
      })}
    </div>
  );
});
