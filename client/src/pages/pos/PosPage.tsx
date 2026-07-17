import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Search, User, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Money } from "@/components/common/Money";
import { NumberInput } from "@/components/common/NumberInput";
import { useToast } from "@/components/ui/use-toast";
import { InvoiceDialog } from "@/components/invoice/InvoiceDialog";
import { CartLines, lineDiscount, type CartLine } from "@/pages/pos/CartLines";
import { ProductGrid } from "@/pages/pos/ProductGrid";
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
import { useDebouncedValue } from "@/lib/hooks/useDebouncedValue";
import { getApiErrorMessage } from "@/lib/http/errors";
import { calculatePricing, clampEditablePrice, type PricingLineInput } from "@/lib/pos/pricing";

const numberFormatter = new Intl.NumberFormat("vi-VN");
const ROUNDING_UNIT = 1000;

interface CartSnapshot {
  cart: CartLine[];
  customer: Customer | null;
  orderDiscountAmount: number;
  voucher: VoucherPreview | null;
  shippingFee: number;
  orderNote: string;
  isDebtSale?: boolean;
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
  // Mac dinh KHACH LE — thanh toan du ngay, khong can chon khach hang (backend chi bat buoc co
  // customer khi con no lai, xem OrderValidationService.assertUnpaidRequiresCustomer). Bat "Ghi
  // nợ" thi bat buoc phai co khach hang da luu, don tao ra voi payments=[] (giu nguyen dung hanh
  // vi cu — toan bo thanh cong no), khac voi truoc day MOI don deu la cong no bat ke co tra tien
  // hay khong (phat hien khi rieng soat, nguoi dung yeu cau sua lai).
  const [isDebtSale, setIsDebtSale] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<"cash" | "bank_transfer" | "card">("cash");
  const [cashReceivedInput, setCashReceivedInput] = useState<number | null>(null);
  const [invoiceToShow, setInvoiceToShow] = useState<number | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  // Giu NGUYEN 1 key cho ca lan bam lai (retry) cua CUNG 1 lan checkout - truoc day sinh key MOI
  // moi lan .mutate() chay, khien co che chong trung phia server (Idempotency-Key bat buoc, xem
  // IdempotencyInterceptor) mat tac dung dung luc can nhat: cashier bam lai "In hoa don" sau khi
  // mat mang/timeout se tao don TRUNG thay vi duoc nhan dien la thu lai cung 1 yeu cau.
  const checkoutIdempotencyKeyRef = useRef<string | null>(null);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Debounce ngan (250ms, ngan hon mac dinh 400ms cua useDebouncedValue) - POS la man hinh nhay
  // cam thoi gian nhat (thu ngan go/quet barcode trong luc khach dang cho), truoc day moi ky tu go
  // vao deu bang 1 request rieng (phat hien khi rieng soat hieu nang); quet barcode van hoat dong
  // binh thuong vi cac ky tu den qua nhanh, chi request DUY NHAT sau ky tu CUOI cung.
  const debouncedSearch = useDebouncedValue(search, 250);
  const debouncedCustomerSearch = useDebouncedValue(customerSearch, 250);

  const productsQuery = useQuery({
    queryKey: ["pos", "products", debouncedSearch, categoryId, branchId],
    queryFn: () =>
      searchProducts({
        search: debouncedSearch,
        categoryId: categoryId === "all" ? undefined : categoryId,
        branchId,
        active: true,
        size: 60,
      }),
  });

  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: listCategories });

  const customersQuery = useQuery({
    queryKey: ["pos", "customers", debouncedCustomerSearch],
    queryFn: () => searchCustomers(debouncedCustomerSearch),
    enabled: debouncedCustomerSearch.trim().length > 0,
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
      if (e.key === "F8") {
        // Nut "Treo don" da ghi san nhan "(F8)" tu truoc nhung chua bao gio noi phim tat that -
        // luon phai bam chuot (phat hien khi ra soat Prompt #5). Giu dung dieu kien disabled cua
        // nut (khong treo don khi gio hang rong hoac dang treo don khac).
        e.preventDefault();
        if (cart.length > 0 && !parkMutation.isPending) {
          parkMutation.mutate();
        }
      }
      if (e.key === "F9") {
        e.preventDefault();
        submitCheckout();
      }
    }
    window.addEventListener("keydown", handleKeydown);
    return () => window.removeEventListener("keydown", handleKeydown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    cart,
    customer,
    orderDiscountAmount,
    appliedVoucher,
    shippingFee,
    isDebtSale,
    paymentMethod,
    cashReceivedInput,
  ]);

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

  // useCallback (khong doi tham chieu moi lan render, deps rong vi chi dung setCart dang
  // "functional update") — bat buoc de ProductGrid/CartLines (boc React.memo) thuc su tranh
  // re-render khi khong lien quan, neu khong callback moi moi lan se lam memo vo tac dung (phat
  // hien qua audit production readiness 2026-07-17).
  const addProduct = useCallback((product: Product) => {
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
  }, []);

  /** Sua gia ban dong (VD: thuong luong voi khach) — kep trong [0, catalogPrice], khong cho tang
   * gia qua gia niem yet (Backend chi nhan chiet khau >=0, khong nhan phu thu — B4). */
  const updatePrice = useCallback((productId: number, rawValue: number) => {
    setCart((prev) =>
      prev.map((l) => {
        if (l.productId !== productId) return l;
        const clamped = clampEditablePrice(rawValue, l.catalogPrice);
        return { ...l, unitPrice: clamped };
      }),
    );
  }, []);

  const removeLine = useCallback((productId: number) => {
    setCart((prev) => prev.filter((l) => l.productId !== productId));
  }, []);

  const updateQuantity = useCallback(
    (productId: number, quantity: number) => {
      if (quantity <= 0) {
        removeLine(productId);
        return;
      }
      setCart((prev) => prev.map((l) => (l.productId === productId ? { ...l, quantity } : l)));
    },
    [removeLine],
  );

  function resetCart() {
    setCart([]);
    setCustomer(null);
    setOrderDiscountAmount(0);
    setVoucherCodeInput("");
    setAppliedVoucher(null);
    setShippingFee(0);
    setOrderNote("");
    setIsDebtSale(false);
    setPaymentMethod("cash");
    setCashReceivedInput(null);
    checkoutIdempotencyKeyRef.current = null;
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

  // Khach le (mac dinh, khong chon khach hang) — luon thanh toan DU ngay, payments = [1 dong bang
  // dung grandTotal]. Ghi no (isDebtSale bat) — bat buoc phai co customer da chon (kiem tra o
  // submitCheckout() ben duoi truoc khi goi mutate, Backend cung tu chan lai o
  // OrderValidationService.assertUnpaidRequiresCustomer neu lot qua), giu NGUYEN dung hanh vi cu:
  // payments=[] (khong thu gi ngay, toan bo thanh cong no phai thu).
  const checkoutMutation = useMutation({
    mutationFn: () => {
      if (!checkoutIdempotencyKeyRef.current) {
        checkoutIdempotencyKeyRef.current = crypto.randomUUID();
      }
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
          payments: isDebtSale ? [] : [{ method: paymentMethod, amount: grandTotal }],
          cashReceived:
            !isDebtSale && paymentMethod === "cash"
              ? (cashReceivedInput ?? grandTotal)
              : undefined,
        },
        checkoutIdempotencyKeyRef.current,
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
    if (isDebtSale && !customer) {
      toast({ variant: "destructive", title: "Ghi nợ bắt buộc phải chọn khách hàng" });
      return;
    }
    if (!isDebtSale && paymentMethod === "cash" && cashReceivedInput != null && cashReceivedInput < grandTotal) {
      toast({ variant: "destructive", title: "Khách đưa chưa đủ tiền" });
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
        isDebtSale,
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
      setIsDebtSale(snapshot.isDebtSale ?? false);
      setPaymentMethod("cash");
      setCashReceivedInput(null);
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

        <ProductGrid products={productsQuery.data?.data ?? []} onAddProduct={addProduct} />
      </div>

      {/* h-full, KHONG overflow-y-auto o day — chi khoi ben trong (khach hang/ghi no/thanh toan/
          ghi chu/gio hang, gop chung 1 vung cuon duy nhat ben duoi) duoc cuon, giu nguyen khu tong
          tien/nut thanh toan luon co dinh o DUOI CUNG man hinh du cuon bao nhieu (nguoi dung yeu
          cau — lan dau chi lam rieng gio hang cuon, ho muon ca khoi thong tin ben tren gio hang
          cung cuon chung, khong dung yen 1 mua rieng). */}
      <div className="flex h-full flex-col gap-2 p-3">
        <div className="min-h-0 flex-1 space-y-2 overflow-y-auto">
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
              placeholder={
                isDebtSale
                  ? "Chọn khách hàng (bắt buộc để ghi nợ) — tìm theo tên/SĐT (F3)"
                  : "Khách lẻ — chọn khách hàng nếu cần ghi nợ (F3)"
              }
              className={isDebtSale ? "border-destructive/50" : undefined}
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

        <div className="flex items-center justify-between rounded-md border px-3 py-2">
          <div>
            <Label className="mb-0 text-sm">Ghi nợ</Label>
            <p className="text-xs text-muted-foreground">
              {isDebtSale
                ? "Không thu tiền ngay — toàn bộ ghi thành công nợ phải thu của khách hàng"
                : "Tắt = khách lẻ, thanh toán đủ ngay"}
            </p>
          </div>
          <Switch checked={isDebtSale} onCheckedChange={setIsDebtSale} />
        </div>

        {!isDebtSale && (
          <div className="flex items-center gap-2">
            <Select value={paymentMethod} onValueChange={(v) => setPaymentMethod(v as typeof paymentMethod)}>
              <SelectTrigger className="h-8 flex-1 text-xs">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="cash">Tiền mặt</SelectItem>
                <SelectItem value="bank_transfer">Chuyển khoản</SelectItem>
                <SelectItem value="card">Thẻ</SelectItem>
              </SelectContent>
            </Select>
            {paymentMethod === "cash" && (
              <NumberInput
                min={0}
                value={cashReceivedInput ?? ""}
                onValueChange={setCashReceivedInput}
                placeholder="Khách đưa"
                className="h-8 w-28 text-right text-xs"
                title="Khách đưa (để trống = đưa vừa đủ)"
              />
            )}
          </div>
        )}
        {!isDebtSale &&
          paymentMethod === "cash" &&
          cashReceivedInput != null &&
          cashReceivedInput > grandTotal && (
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>Tiền thừa trả khách</span>
              <Money value={cashReceivedInput - grandTotal} />
            </div>
          )}

        <Input
          value={orderNote}
          onChange={(e) => setOrderNote(e.target.value)}
          placeholder="Ghi chú đơn hàng (tùy chọn)"
          className="h-8 text-xs"
        />

        <div className="space-y-1.5">
          <CartLines
            cart={cart}
            onUpdatePrice={updatePrice}
            onUpdateQuantity={updateQuantity}
            onRemoveLine={removeLine}
          />
        </div>
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
            <NumberInput
              // min chan that su (khong chi goi y HTML nhu <input type=number> truoc day) - ky tu
              // "-" bi loc bo ngay tu luc go, khong con duong nao lot so am vao state (phat hien
              // khi rieng soat, ap dung ca cho Phi ship ben duoi).
              min={0}
              value={orderDiscountAmount}
              onValueChange={(v) => setOrderDiscountAmount(v ?? 0)}
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
            <NumberInput
              min={0}
              value={shippingFee}
              onValueChange={(v) => setShippingFee(v ?? 0)}
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
            disabled={cart.length === 0 || (isDebtSale && !customer) || checkoutMutation.isPending}
            onClick={submitCheckout}
            title={isDebtSale && !customer ? "Ghi nợ cần chọn khách hàng trước" : undefined}
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
