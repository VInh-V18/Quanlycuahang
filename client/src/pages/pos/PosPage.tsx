import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus, Search, User, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Money } from "@/components/common/Money";
import { useToast } from "@/components/ui/use-toast";
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
import { validateVoucher, type VoucherPreview } from "@/lib/api/vouchers";
import { CURRENT_BRANCH_ID } from "@/lib/constants";
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
  unitPrice: number;
  vatRate: number;
  quantity: number;
  lineDiscountAmount: number;
  stock: number;
}

interface CartSnapshot {
  cart: CartLine[];
  customer: Customer | null;
  orderDiscountAmount: number;
  voucher: VoucherPreview | null;
}

export function PosPage() {
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<number | "all">("all");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [customerSearch, setCustomerSearch] = useState("");
  const [showCustomerSearch, setShowCustomerSearch] = useState(false);
  const [orderDiscountAmount, setOrderDiscountAmount] = useState(0);
  const [voucherCodeInput, setVoucherCodeInput] = useState("");
  const [appliedVoucher, setAppliedVoucher] = useState<VoucherPreview | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<"cash" | "bank_transfer" | "debt">("cash");
  const [cashReceived, setCashReceived] = useState(0);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const productsQuery = useQuery({
    queryKey: ["pos", "products", search, categoryId],
    queryFn: () =>
      searchProducts({
        search,
        categoryId: categoryId === "all" ? undefined : categoryId,
        branchId: CURRENT_BRANCH_ID,
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
    queryKey: ["parked-orders", CURRENT_BRANCH_ID],
    queryFn: () => listParkedOrders(CURRENT_BRANCH_ID),
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
  }, [cart, paymentMethod, cashReceived, customer, orderDiscountAmount, appliedVoucher]);

  const pricingLines: PricingLineInput[] = cart.map((l) => ({
    productId: l.productId,
    unitPrice: l.unitPrice,
    quantity: l.quantity,
    lineDiscountAmount: l.lineDiscountAmount,
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
        cashReceived: paymentMethod === "cash" ? cashReceived : null,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [cart, orderDiscountAmount, appliedVoucher, paymentMethod, cashReceived],
  );

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
          vatRate: product.vatRate,
          quantity: 1,
          lineDiscountAmount: 0,
          stock: product.stock ?? 0,
        },
      ];
    });
  }

  function updateQuantity(productId: number, quantity: number) {
    if (quantity <= 0) {
      removeLine(productId);
      return;
    }
    setCart((prev) => prev.map((l) => (l.productId === productId ? { ...l, quantity } : l)));
  }

  function updateLineDiscount(productId: number, amount: number) {
    setCart((prev) =>
      prev.map((l) => (l.productId === productId ? { ...l, lineDiscountAmount: amount } : l)),
    );
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
    setPaymentMethod("cash");
    setCashReceived(0);
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

  const checkoutMutation = useMutation({
    mutationFn: () => {
      const payments =
        paymentMethod === "debt"
          ? []
          : [{ method: paymentMethod, amount: pricing.totalAmount }];
      return createOrder(
        {
          branchId: CURRENT_BRANCH_ID,
          customerId: customer?.id,
          voucherCode: appliedVoucher?.code,
          orderDiscountAmount,
          cashReceived: paymentMethod === "cash" ? cashReceived : undefined,
          expectedTotalAmount: pricing.totalAmount,
          lines: cart.map((l) => ({
            productId: l.productId,
            quantity: l.quantity,
            lineDiscountAmount: l.lineDiscountAmount,
          })),
          payments,
        },
        crypto.randomUUID(),
      );
    },
    onSuccess: (order) => {
      toast({ title: `Đã thanh toán đơn ${order.orderNumber}` });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      if (order.invoiceId) {
        window.open(`/invoices/${order.invoiceId}/print`, "_blank");
      }
      resetCart();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể thanh toán", description: getApiErrorMessage(err) });
    },
  });

  function submitCheckout() {
    if (cart.length === 0 || checkoutMutation.isPending) return;
    if (paymentMethod === "debt" && !customer) {
      toast({ variant: "destructive", title: "Ghi nợ cần chọn khách hàng" });
      return;
    }
    if (paymentMethod === "cash" && cashReceived < pricing.totalAmount) {
      toast({ variant: "destructive", title: "Khách đưa chưa đủ tiền" });
      return;
    }
    checkoutMutation.mutate();
  }

  const parkMutation = useMutation({
    mutationFn: () => {
      const snapshot: CartSnapshot = { cart, customer, orderDiscountAmount, voucher: appliedVoucher };
      return parkOrder(CURRENT_BRANCH_ID, JSON.stringify(snapshot));
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
      queryClient.invalidateQueries({ queryKey: ["parked-orders"] });
      toast({ title: "Đã mở lại đơn treo" });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể mở đơn treo", description: getApiErrorMessage(err) });
    },
  });

  return (
    <div className="grid h-full grid-cols-1 md:grid-cols-[60%_40%]">
      <div className="flex flex-col gap-3 overflow-y-auto border-r p-4">
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

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {productsQuery.data?.data.map((product) => {
            const outOfStock = (product.stock ?? 0) <= 0;
            return (
              <button
                key={product.id}
                type="button"
                disabled={outOfStock}
                onClick={() => addProduct(product)}
                className="flex flex-col items-start gap-1 rounded-lg border bg-card p-3 text-left transition-colors hover:border-primary disabled:cursor-not-allowed disabled:opacity-50"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-md bg-accent text-2xl">
                  {categoryEmoji(product.categoryName)}
                </div>
                <div className="text-sm font-medium leading-tight">{product.name}</div>
                <div className="text-sm font-semibold text-primary">
                  <Money value={product.sellPrice} />/{product.unit}
                </div>
                <div className={`text-xs ${outOfStock ? "text-destructive" : "text-muted-foreground"}`}>
                  {outOfStock ? "Hết hàng" : `Tồn ${numberFormatter.format(product.stock ?? 0)}`}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex flex-col gap-3 overflow-y-auto p-4">
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
              placeholder="Khách lẻ — tìm khách hàng (F3)"
            />
          )}
          {showCustomerSearch && customerSearch.trim() && (customersQuery.data?.data.length ?? 0) > 0 && (
            <div className="absolute z-10 mt-1 w-full rounded-md border bg-popover shadow-md">
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

        <div className="flex-1 space-y-2">
          {cart.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">Giỏ hàng trống</p>
          ) : (
            cart.map((line, index) => (
              <div key={line.productId} className="flex items-start justify-between gap-2 border-b pb-2">
                <div className="flex-1">
                  <div className="text-sm font-medium">
                    {index + 1}. {line.name}
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Money value={line.unitPrice} /> × {numberFormatter.format(line.quantity)} {line.unit}
                    {line.lineDiscountAmount > 0 && (
                      <span>· CK: −{numberFormatter.format(line.lineDiscountAmount)}</span>
                    )}
                  </div>
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
                    <Money value={line.unitPrice * line.quantity - line.lineDiscountAmount} />
                  </div>
                  <button
                    onClick={() => removeLine(line.productId)}
                    className="mt-1 text-muted-foreground hover:text-destructive"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="space-y-1 border-t pt-3 text-sm">
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
        </div>

        <div className="flex items-center justify-between border-t pt-3">
          <span className="text-lg font-semibold">KHÁCH PHẢI TRẢ</span>
          <span className="text-2xl font-bold text-primary">
            <Money value={pricing.totalAmount} />
          </span>
        </div>

        <div className="grid grid-cols-3 gap-2">
          <Button
            variant={paymentMethod === "cash" ? "default" : "outline"}
            onClick={() => setPaymentMethod("cash")}
          >
            Tiền mặt
          </Button>
          <Button
            variant={paymentMethod === "bank_transfer" ? "default" : "outline"}
            onClick={() => setPaymentMethod("bank_transfer")}
          >
            CK · VietQR
          </Button>
          <Button
            variant={paymentMethod === "debt" ? "default" : "outline"}
            onClick={() => setPaymentMethod("debt")}
          >
            Ghi nợ
          </Button>
        </div>

        {paymentMethod === "cash" && (
          <div className="grid grid-cols-2 gap-2 text-sm">
            <div>
              <label className="text-muted-foreground">Khách đưa</label>
              <Input
                type="number"
                min={0}
                value={cashReceived}
                onChange={(e) => setCashReceived(Number(e.target.value))}
                className="mt-1"
              />
            </div>
            <div>
              <label className="text-muted-foreground">Tiền thừa</label>
              <div className="mt-1 flex h-10 items-center rounded-md border bg-muted px-3 font-medium">
                <Money value={Math.max(0, cashReceived - pricing.totalAmount)} />
              </div>
            </div>
          </div>
        )}

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
            disabled={cart.length === 0 || checkoutMutation.isPending}
            onClick={submitCheckout}
          >
            Thanh toán &amp; In (F9)
          </Button>
        </div>
      </div>
    </div>
  );
}
