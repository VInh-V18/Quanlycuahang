import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus, Search, User, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Money } from "@/components/common/Money";
import { useToast } from "@/components/ui/use-toast";
import { InvoiceDialog } from "@/components/invoice/InvoiceDialog";
import { listCategories } from "@/lib/api/categories";
import { searchCustomers, type Customer } from "@/lib/api/customers";
import { createOrder } from "@/lib/api/orders";
import {
  listParkedOrders,
  parkOrder,
  resumeParkedOrder,
  type ParkedOrder,
} from "@/lib/api/parkedOrders";
import { searchProducts, type Product } from "@/lib/api/products";
import { getCurrentShift } from "@/lib/api/shifts";
import { validateVoucher, type VoucherPreview } from "@/lib/api/vouchers";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { getApiErrorMessage } from "@/lib/http/errors";
import { categoryEmoji } from "@/lib/pos/categoryEmoji";
import { calculatePricing, type PricingLineInput } from "@/lib/pos/pricing";

const numberFormatter = new Intl.NumberFormat("vi-VN");
const ROUNDING_UNIT = 1000;

interface CartLine {
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

interface CartSnapshot {
  cart: CartLine[];
  customer: Customer | null;
  orderDiscountAmount: number;
  voucher: VoucherPreview | null;
  shippingFee: number;
  orderNote: string;
}

/** Chiet khau dong suy ra tu chenh lech gia ban hien tai voi gia goc — tinh lai moi lan (khong luu
 * rieng) de luon khop dung khi so luong doi sau khi da sua gia. */
function lineDiscount(line: CartLine): number {
  const catalogPrice = line.catalogPrice ?? line.unitPrice;
  return Math.max(0, Math.round((catalogPrice - line.unitPrice) * line.quantity));
}

export function PosPage() {
  const branchId = useCurrentBranchId();
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<number | "all">("all");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [customerSearch, setCustomerSearch] = useState("");
  const [showCustomerSearch, setShowCustomerSearch] = useState(false);
  const [orderDiscountAmount, setOrderDiscountAmount] = useState(0);
  const [voucherCodeInput, setVoucherCodeInput] = useState("");
  const [appliedVoucher, setAppliedVoucher] = useState<VoucherPreview | null>(null);
  const [shippingFee, setShippingFee] = useState(0);
  const [orderNote, setOrderNote] = useState("");
  const [invoiceToShow, setInvoiceToShow] = useState<number | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const productsQuery = useQuery({
    queryKey: ["pos", "products", search, categoryId, branchId],
    queryFn: () =>
      searchProducts({
        search,
        categoryId: categoryId === "all" ? undefined : categoryId,
        branchId,
        active: true,
        size: 60,
      }),
  });

  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: listCategories });

  const customersQuery = useQuery({
    queryKey: ["pos", "customers", customerSearch],
    queryFn: () => searchCustomers(customerSearch),
    enabled: customerSearch.trim().length > 0,
  });

  const parkedOrdersQuery = useQuery({
    queryKey: ["parked-orders", branchId],
    queryFn: () => listParkedOrders(branchId),
  });

  useEffect(() => {
    function handleKeydown(e: KeyboardEvent) {
      if (e.key === "F1") {
        e.preventDefault();
        searchInputRef.current?.focus();
      }
      if (e.key === "F9") {
        e.preventDefault();
        submitCheckout();
      }
    }
    window.addEventListener("keydown", handleKeydown);
    return () => window.removeEventListener("keydown", handleKeydown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cart, customer, orderDiscountAmount, appliedVoucher, shippingFee]);

  // unitPrice gui vao pricing engine la GIA GOC (catalogPrice), chenh lech voi gia da sua tay
  // (l.unitPrice) the hien qua lineDiscount() — dung cach Backend hieu chiet khau dong (B4),
  // khong phai gia moi thay the thang gia goc.
  const pricingLines: PricingLineInput[] = cart.map((l) => ({
    productId: l.productId,
    unitPrice: l.catalogPrice,
    quantity: l.quantity,
    lineDiscountAmount: lineDiscount(l),
    vatRate: l.vatRate,
  }));

  const pricing = useMemo(
    () =>
      calculatePricing({
        lines: pricingLines,
        orderDiscountAmount,
        voucherAmount: appliedVoucher?.discountAmount ?? 0,
        priceIncludesVat: true,
        roundingUnit: ROUNDING_UNIT,
        cashReceived: null,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [cart, orderDiscountAmount, appliedVoucher],
  );

  // Phi ship cong them SAU pricing (khong qua CK/VAT/lam tron hang hoa) — khop cong thuc Backend
  // (OrderService.createOrder), xem V9__order_shipping_fee_and_note.sql.
  const grandTotal = pricing.totalAmount + shippingFee;

  const totalQuantity = cart.reduce((sum, l) => sum + l.quantity, 0);

  function addProduct(product: Product) {
    setCart((prev) => {
      const existing = prev.find((l) => l.productId === product.id);
      if (existing) {
        return prev.map((l) =>
          l.productId === product.id ? { ...l, quantity: l.quantity + 1 } : l,
        );
      }
      return [
        ...prev,
        {
          productId: product.id,
          name: product.name,
          sku: product.sku,
          unit: product.unit,
          unitPrice: product.sellPrice,
          catalogPrice: product.sellPrice,
          costPrice: product.costPrice,
          vatRate: product.vatRate,
          quantity: 1,
          stock: product.stock ?? 0,
        },
      ];
    });
  }

  /** Sua gia ban dong (VD: thuong luong voi khach) — kep trong [0, catalogPrice], khong cho tang
   * gia qua gia niem yet (Backend chi nhan chiet khau >=0, khong nhan phu thu — B4). */
  function updatePrice(productId: number, rawValue: number) {
    setCart((prev) =>
      prev.map((l) => {
        if (l.productId !== productId) return l;
        const clamped = Math.min(l.catalogPrice, Math.max(0, rawValue || 0));
        return { ...l, unitPrice: clamped };
      }),
    );
  }

  function updateQuantity(productId: number, quantity: number) {
    if (quantity <= 0) {
      removeLine(productId);
      return;
    }
    setCart((prev) => prev.map((l) => (l.productId === productId ? { ...l, quantity } : l)));
  }

  function removeLine(productId: number) {
    setCart((prev) => prev.filter((l) => l.productId !== productId));
  }

  function resetCart() {
    setCart([]);
    setCustomer(null);
    setOrderDiscountAmount(0);
    setVoucherCodeInput("");
    setAppliedVoucher(null);
    setShippingFee(0);
    setOrderNote("");
  }

  const voucherMutation = useMutation({
    mutationFn: () => validateVoucher(voucherCodeInput.trim(), pricing.subtotal),
    onSuccess: (result) => {
      setAppliedVoucher(result);
      toast({ title: `Đã áp dụng voucher ${result.code}` });
    },
    onError: (err) => {
      setAppliedVoucher(null);
      toast({ variant: "destructive", title: "Voucher không hợp lệ", description: getApiErrorMessage(err) });
    },
  });

  const currentShiftQuery = useQuery({ queryKey: ["shifts", "current"], queryFn: getCurrentShift });

  // Khong con thu tien tai POS (bo tien mat/CK/ghi no) — moi don deu ghi thanh cong no phai thu
  // cua khach hang (payments=[]), nen bat buoc phai co customer (xem submitCheckout() ben duoi va
  // OrderService: unpaid > 0 && customer == null se bi Backend tu choi).
  const checkoutMutation = useMutation({
    mutationFn: () => {
      return createOrder(
        {
          branchId,
          customerId: customer?.id,
          shiftId: currentShiftQuery.data?.id,
          voucherCode: appliedVoucher?.code,
          orderDiscountAmount,
          expectedTotalAmount: grandTotal,
          shippingFee,
          note: orderNote.trim() || undefined,
          lines: cart.map((l) => ({
            productId: l.productId,
            quantity: l.quantity,
            lineDiscountAmount: lineDiscount(l),
          })),
          payments: [],
        },
        crypto.randomUUID(),
      );
    },
    onSuccess: (order) => {
      toast({ title: `Đã tạo đơn ${order.orderNumber}` });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      queryClient.invalidateQueries({ queryKey: ["shifts"] });
      queryClient.invalidateQueries({ queryKey: ["debts"] });
      if (order.invoiceId) {
        setInvoiceToShow(order.invoiceId);
      }
      resetCart();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể tạo đơn", description: getApiErrorMessage(err) });
    },
  });

  function submitCheckout() {
    if (cart.length === 0 || checkoutMutation.isPending) return;
    if (!customer) {
      toast({ variant: "destructive", title: "Vui lòng chọn khách hàng trước khi in hóa đơn" });
      return;
    }
    checkoutMutation.mutate();
  }

  const parkMutation = useMutation({
    mutationFn: () => {
      const snapshot: CartSnapshot = {
        cart,
        customer,
        orderDiscountAmount,
        voucher: appliedVoucher,
        shippingFee,
        orderNote,
      };
      return parkOrder(branchId, JSON.stringify(snapshot));
    },
    onSuccess: () => {
      toast({ title: "Đã treo đơn" });
      queryClient.invalidateQueries({ queryKey: ["parked-orders"] });
      resetCart();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể treo đơn", description: getApiErrorMessage(err) });
    },
  });

  const resumeMutation = useMutation({
    mutationFn: (id: number) => resumeParkedOrder(id),
    onSuccess: (parked) => {
      const snapshot = JSON.parse(parked.cartSnapshot) as CartSnapshot;
      setCart(snapshot.cart);
      setCustomer(snapshot.customer);
      setOrderDiscountAmount(snapshot.orderDiscountAmount);
      setAppliedVoucher(snapshot.voucher);
      setShippingFee(snapshot.shippingFee ?? 0);
      setOrderNote(snapshot.orderNote ?? "");
      queryClient.invalidateQueries({ queryKey: ["parked-orders"] });
      toast({ title: "Đã mở lại đơn treo" });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể mở đơn treo", description: getApiErrorMessage(err) });
    },
  });

  return (
    <div className="-m-6 grid h-[calc(100%+3rem)] grid-cols-1 print:hidden md:grid-cols-[60%_40%]">
      <div className="flex flex-col gap-2 overflow-y-auto border-r p-3">
        {parkedOrdersQuery.data && parkedOrdersQuery.data.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {parkedOrdersQuery.data.map((parked: ParkedOrder) => (
              <Button
                key={parked.id}
                variant="secondary"
                size="sm"
                onClick={() => resumeMutation.mutate(parked.id)}
              >
                Đơn treo #{parked.id}
                {parked.note && ` — ${parked.note}`}
              </Button>
            ))}
          </div>
        )}

        <div className="relative">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            ref={searchInputRef}
            autoFocus
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm tên, SKU hoặc quét barcode... (F1)"
            className="pl-8"
          />
        </div>

        <div className="flex flex-wrap gap-2">
          <Button
            variant={categoryId === "all" ? "default" : "outline"}
            size="sm"
            onClick={() => setCategoryId("all")}
          >
            Tất cả
          </Button>
          {categoriesQuery.data?.map((c) => (
            <Button
              key={c.id}
              variant={categoryId === c.id ? "default" : "outline"}
              size="sm"
              onClick={() => setCategoryId(c.id)}
            >
              {c.name}
            </Button>
          ))}
        </div>

        <div className="flex flex-col divide-y rounded-lg border">
          {productsQuery.data?.data.map((product) => {
            const outOfStock = (product.stock ?? 0) <= 0;
            return (
              <button
                key={product.id}
                type="button"
                disabled={outOfStock}
                onClick={() => addProduct(product)}
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
      </div>

      <div className="flex flex-col gap-2 overflow-y-auto p-3">
        <div className="relative">
          {customer ? (
            <div className="flex items-center justify-between rounded-md border px-3 py-2 text-sm">
              <span>
                <User className="mr-1.5 inline h-4 w-4" />
                {customer.name} {customer.phone && `· ${customer.phone}`}
              </span>
              <button onClick={() => setCustomer(null)} className="text-muted-foreground hover:text-destructive">
                <X className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <Input
              value={customerSearch}
              onChange={(e) => {
                setCustomerSearch(e.target.value);
                setShowCustomerSearch(true);
              }}
              onFocus={() => setShowCustomerSearch(true)}
              placeholder="Chọn khách hàng (bắt buộc) — tìm theo tên/SĐT (F3)"
              className="border-destructive/50"
            />
          )}
          {showCustomerSearch && customerSearch.trim() && (customersQuery.data?.data.length ?? 0) > 0 && (
            <div className="absolute z-50 mt-1 w-full rounded-md border bg-background shadow-md">
              {customersQuery.data?.data.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  className="block w-full px-3 py-2 text-left text-sm hover:bg-accent"
                  onClick={() => {
                    setCustomer(c);
                    setCustomerSearch("");
                    setShowCustomerSearch(false);
                  }}
                >
                  {c.name} {c.phone && `· ${c.phone}`}
                </button>
              ))}
            </div>
          )}
        </div>

        <Input
          value={orderNote}
          onChange={(e) => setOrderNote(e.target.value)}
          placeholder="Ghi chú đơn hàng (tùy chọn)"
          className="h-8 text-xs"
        />

        <div className="flex-1 space-y-1.5">
          {cart.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">Giỏ hàng trống</p>
          ) : (
            cart.map((line, index) => {
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
                    <Input
                      type="number"
                      min={0}
                      max={line.catalogPrice}
                      value={line.unitPrice}
                      onChange={(e) => updatePrice(line.productId, Number(e.target.value))}
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
                  <div className="mt-1 flex items-center gap-1">
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-6 w-6"
                      onClick={() => updateQuantity(line.productId, line.quantity - 1)}
                    >
                      <Minus className="h-3 w-3" />
                    </Button>
                    <Input
                      type="number"
                      value={line.quantity}
                      onChange={(e) => updateQuantity(line.productId, Number(e.target.value))}
                      className="h-6 w-14 text-center text-xs"
                    />
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-6 w-6"
                      onClick={() => updateQuantity(line.productId, line.quantity + 1)}
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
                    onClick={() => removeLine(line.productId)}
                    className="mt-1 text-muted-foreground hover:text-destructive"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </div>
              );
            })
          )}
        </div>

        <div className="space-y-1 border-t pt-2 text-sm">
          <div className="flex justify-between text-muted-foreground">
            <span>
              Tổng hàng ({cart.length} mặt hàng · {numberFormatter.format(totalQuantity)})
            </span>
            <Money value={pricing.subtotal} />
          </div>
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted-foreground">Chiết khấu đơn</span>
            <Input
              type="number"
              min={0}
              value={orderDiscountAmount}
              onChange={(e) => setOrderDiscountAmount(Number(e.target.value))}
              className="h-7 w-28 text-right"
            />
          </div>
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted-foreground">Voucher</span>
            <div className="flex gap-1">
              <Input
                value={voucherCodeInput}
                onChange={(e) => setVoucherCodeInput(e.target.value.toUpperCase())}
                placeholder="Mã voucher"
                className="h-7 w-28"
              />
              <Button
                size="sm"
                variant="outline"
                className="h-7"
                disabled={!voucherCodeInput.trim() || voucherMutation.isPending}
                onClick={() => voucherMutation.mutate()}
              >
                Áp dụng
              </Button>
            </div>
          </div>
          {appliedVoucher && (
            <div className="flex justify-between text-destructive">
              <span>Voucher {appliedVoucher.code}</span>
              <span>−{numberFormatter.format(appliedVoucher.discountAmount)}</span>
            </div>
          )}
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>VAT đã gồm trong giá</span>
            <span>{numberFormatter.format(pricing.totalVat)}</span>
          </div>
          {pricing.roundingAdjustment !== 0 && (
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>Làm tròn</span>
              <span>{numberFormatter.format(pricing.roundingAdjustment)}</span>
            </div>
          )}
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted-foreground">Phí ship</span>
            <Input
              type="number"
              min={0}
              value={shippingFee}
              onChange={(e) => setShippingFee(Number(e.target.value))}
              className="h-7 w-28 text-right"
            />
          </div>
        </div>

        <div className="flex items-center justify-between border-t pt-2">
          <span className="text-lg font-semibold">KHÁCH PHẢI TRẢ</span>
          <span className="text-2xl font-bold text-primary">
            <Money value={grandTotal} />
          </span>
        </div>

        <div className="flex gap-2">
          <Button
            variant="outline"
            className="flex-1"
            disabled={cart.length === 0 || parkMutation.isPending}
            onClick={() => parkMutation.mutate()}
          >
            Treo đơn (F8)
          </Button>
          <Button
            className="flex-1"
            size="lg"
            disabled={cart.length === 0 || !customer || checkoutMutation.isPending}
            onClick={submitCheckout}
            title={!customer ? "Cần chọn khách hàng trước" : undefined}
          >
            In hóa đơn (F9)
          </Button>
        </div>
      </div>

      {invoiceToShow != null && (
        <InvoiceDialog invoiceId={invoiceToShow} onClose={() => setInvoiceToShow(null)} />
      )}
    </div>
  );
}
