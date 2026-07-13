import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Bot, Search, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Money } from "@/components/common/Money";
import { PermissionGate } from "@/components/common/PermissionGate";
import { useToast } from "@/components/ui/use-toast";
import {
  getAdvancedPurchaseSuggestions,
  getPurchaseSuggestions,
  type PurchaseSuggestion,
} from "@/lib/api/ai";
import { createPurchaseOrder } from "@/lib/api/purchaseOrders";
import { getProduct, searchProducts, type Product } from "@/lib/api/products";
import { listSuppliers } from "@/lib/api/suppliers";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { useDebouncedValue } from "@/lib/hooks/useDebouncedValue";
import { getApiErrorMessage } from "@/lib/http/errors";

interface DraftLine {
  product: Product;
  quantity: number;
  unitPrice: number;
  batchCode: string;
  expiryDate: string;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

function calculateNewCost(currentStock: number, currentCost: number, qty: number, price: number) {
  const totalStock = currentStock + qty;
  if (totalStock <= 0) return Math.round(price);
  return Math.round((currentStock * currentCost + qty * price) / totalStock);
}

/** Danh sach goi y nhap hang tu thuat toan xac dinh (Prompt #11, tinh nang dot 1b - xem Javadoc
 * AiPurchaseSuggestionService, KHONG phai LLM) - nguoi dung XEM va TU BAM THEM tung dong, khong co
 * dong nao duoc tu dong dua vao phieu ma khong qua xac nhan. */
function AiSuggestionsDialog({
  branchId,
  mode,
  existingProductIds,
  onAdd,
  onClose,
}: {
  branchId: number;
  /** "advanced" (Prompt #12) qua ml-service (IsolationForest, hien them % do tin cay) - Backend tu
   * fallback ve cong thuc don gian neu ml-service khong kha dung, FE khong can tu xu ly rieng. */
  mode: "simple" | "advanced";
  existingProductIds: number[];
  onAdd: (product: Product, quantity: number) => void;
  onClose: () => void;
}) {
  const [addedIds, setAddedIds] = useState<number[]>([]);
  const { toast } = useToast();

  const suggestionsQuery = useQuery({
    queryKey: ["ai", "purchase-suggestions", mode, branchId],
    queryFn: () =>
      mode === "advanced" ? getAdvancedPurchaseSuggestions(branchId) : getPurchaseSuggestions(branchId),
  });

  const addMutation = useMutation({
    mutationFn: async (suggestion: PurchaseSuggestion) => {
      const product = await getProduct(suggestion.productId, branchId);
      return { product, quantity: suggestion.suggestedQty };
    },
    onSuccess: ({ product, quantity }) => {
      onAdd(product, quantity);
      setAddedIds((prev) => [...prev, product.id]);
    },
    onError: (err) => {
      toast({
        variant: "destructive",
        title: "Không thể thêm sản phẩm",
        description: getApiErrorMessage(err),
      });
    },
  });

  const suggestions = suggestionsQuery.data ?? [];

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Bot className="h-5 w-5" />
            {mode === "advanced" ? "Gợi ý nhập hàng AI nâng cao" : "Gợi ý nhập hàng từ AI"}
          </DialogTitle>
          <DialogDescription>
            {mode === "advanced"
              ? "Dựa trên mô hình phân tích 90 ngày bán gần đây (đã lọc ngày bán bất thường). Xem lại và bấm \"Thêm\" cho từng sản phẩm bạn muốn đưa vào phiếu."
              : "Dựa trên tốc độ bán 30 ngày gần đây và định mức tồn tối thiểu. Xem lại và bấm \"Thêm\" cho từng sản phẩm bạn muốn đưa vào phiếu."}
          </DialogDescription>
        </DialogHeader>

        <div className="max-h-[60vh] space-y-2 overflow-y-auto">
          {suggestionsQuery.isLoading && (
            <p className="py-6 text-center text-sm text-muted-foreground">Đang lấy gợi ý...</p>
          )}
          {suggestionsQuery.isError && (
            <p className="py-6 text-center text-sm text-destructive">
              {getApiErrorMessage(suggestionsQuery.error, "Không thể lấy gợi ý nhập hàng")}
            </p>
          )}
          {suggestionsQuery.isSuccess && suggestions.length === 0 && (
            <p className="py-6 text-center text-sm text-muted-foreground">
              Không có sản phẩm nào cần nhập thêm lúc này
            </p>
          )}
          {suggestions.map((s) => {
            const alreadyInOrder = existingProductIds.includes(s.productId) || addedIds.includes(s.productId);
            return (
              <div key={s.productId} className="flex items-center justify-between gap-2 rounded-md border p-2">
                <div className="min-w-0">
                  <div className="truncate font-medium">{s.productName}</div>
                  <div className="text-xs text-muted-foreground">
                    {s.sku} · Tồn {s.currentStock}/{s.minStock} · Gợi ý nhập {s.suggestedQty}
                    {s.confidence != null && ` · Độ tin cậy ${Math.round(s.confidence * 100)}%`}
                  </div>
                </div>
                <Button
                  size="sm"
                  variant={alreadyInOrder ? "outline" : "default"}
                  disabled={alreadyInOrder || (addMutation.isPending && addMutation.variables?.productId === s.productId)}
                  onClick={() => addMutation.mutate(s)}
                >
                  {alreadyInOrder ? "Đã thêm" : "Thêm"}
                </Button>
              </div>
            );
          })}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Đóng
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function PurchaseOrderCreatePage() {
  const branchId = useCurrentBranchId();
  const [search, setSearch] = useState("");
  const [lines, setLines] = useState<DraftLine[]>([]);
  const [supplierId, setSupplierId] = useState<string>("");
  const [discountAmount, setDiscountAmount] = useState(0);
  const [paidAmount, setPaidAmount] = useState(0);
  const [aiSuggestionsMode, setAiSuggestionsMode] = useState<"simple" | "advanced" | null>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const debouncedSearch = useDebouncedValue(search);

  const searchQuery = useQuery({
    queryKey: ["products", "search-to-add", debouncedSearch, branchId],
    queryFn: () => searchProducts({ search: debouncedSearch, page: 0, size: 8, branchId }),
    enabled: debouncedSearch.trim().length > 0,
  });

  const suppliersQuery = useQuery({ queryKey: ["suppliers"], queryFn: listSuppliers });
  const supplier = suppliersQuery.data?.data.find((s) => String(s.id) === supplierId);

  const totalAmount = useMemo(
    () => lines.reduce((sum, l) => sum + l.quantity * l.unitPrice, 0),
    [lines],
  );
  const payable = Math.max(0, totalAmount - discountAmount);
  const debtAmount = Math.max(0, payable - paidAmount);

  function addProduct(product: Product, quantity = 1) {
    if (lines.some((l) => l.product.id === product.id)) return;
    setLines((prev) => [
      ...prev,
      { product, quantity, unitPrice: product.costPrice ?? product.sellPrice, batchCode: "", expiryDate: "" },
    ]);
    setSearch("");
  }

  function updateLine(productId: number, patch: Partial<DraftLine>) {
    setLines((prev) => prev.map((l) => (l.product.id === productId ? { ...l, ...patch } : l)));
  }

  function removeLine(productId: number) {
    setLines((prev) => prev.filter((l) => l.product.id !== productId));
  }

  const createMutation = useMutation({
    mutationFn: () =>
      createPurchaseOrder({
        supplierId: Number(supplierId),
        branchId,
        discountAmount,
        paidAmount,
        items: lines.map((l) => ({
          productId: l.product.id,
          quantity: l.quantity,
          unitPrice: l.unitPrice,
          batchCode: l.batchCode || undefined,
          expiryDate: l.expiryDate || undefined,
        })),
      }),
    onSuccess: () => {
      toast({ title: "Đã hoàn tất nhập kho" });
      queryClient.invalidateQueries({ queryKey: ["purchase-orders"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      navigate("/purchase-orders");
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể nhập kho", description: getApiErrorMessage(err) });
    },
  });

  const canSubmit = !!supplierId && lines.length > 0 && !createMutation.isPending;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Tạo phiếu nhập</h1>
          <p className="text-sm text-muted-foreground">Nhập kho / Tạo phiếu</p>
        </div>
        <div className="flex items-center gap-2">
          <PermissionGate perm="ai:use">
            <Button variant="outline" onClick={() => setAiSuggestionsMode("simple")}>
              <Bot className="h-4 w-4" />
              Gợi ý từ AI
            </Button>
            <Button variant="outline" onClick={() => setAiSuggestionsMode("advanced")}>
              <Bot className="h-4 w-4" />
              Gợi ý AI nâng cao
            </Button>
          </PermissionGate>
          <Button size="lg" disabled={!canSubmit} onClick={() => createMutation.mutate()}>
            Hoàn tất nhập kho
          </Button>
        </div>
      </div>

      {aiSuggestionsMode && (
        <AiSuggestionsDialog
          branchId={branchId}
          mode={aiSuggestionsMode}
          existingProductIds={lines.map((l) => l.product.id)}
          onAdd={(product, quantity) => addProduct(product, quantity)}
          onClose={() => setAiSuggestionsMode(null)}
        />
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Card>
            <CardContent className="space-y-4 pt-6">
              <div className="relative">
                <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Thêm sản phẩm vào phiếu — tên / SKU / quét barcode"
                  className="pl-8"
                />
                {search.trim() && (searchQuery.data?.data.length ?? 0) > 0 && (
                  <div className="absolute z-50 mt-1 w-full rounded-md border bg-background shadow-md">
                    {searchQuery.data?.data.map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-accent"
                        onClick={() => addProduct(p)}
                      >
                        <span>
                          {p.name} <span className="text-muted-foreground">· {p.sku}</span>
                        </span>
                        <Money value={p.sellPrice} className="text-muted-foreground" />
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {lines.length === 0 ? (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  Chưa có sản phẩm nào trong phiếu
                </p>
              ) : (
                <div className="space-y-3">
                  {lines.map((line) => {
                    const newCost = calculateNewCost(
                      line.product.stock ?? 0,
                      line.product.costPrice ?? 0,
                      line.quantity,
                      line.unitPrice,
                    );
                    return (
                      <div key={line.product.id} className="rounded-md border p-3">
                        <div className="mb-2 flex items-start justify-between gap-2">
                          <div>
                            <div className="font-medium">{line.product.name}</div>
                            <div className="text-xs text-muted-foreground">
                              {line.product.sku} · Tồn {line.product.stock ?? 0} {line.product.unit}
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => removeLine(line.product.id)}
                            className="text-muted-foreground hover:text-destructive"
                          >
                            <X className="h-4 w-4" />
                          </button>
                        </div>
                        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                          <div>
                            <label className="text-xs text-muted-foreground">Lô</label>
                            <Input
                              value={line.batchCode}
                              onChange={(e) => updateLine(line.product.id, { batchCode: e.target.value })}
                              placeholder="VD: CH-0407"
                            />
                          </div>
                          <div>
                            <label className="text-xs text-muted-foreground">HSD</label>
                            <Input
                              type="date"
                              value={line.expiryDate}
                              onChange={(e) => updateLine(line.product.id, { expiryDate: e.target.value })}
                            />
                          </div>
                          <div>
                            <label className="text-xs text-muted-foreground">SL nhập</label>
                            <Input
                              type="number"
                              min={0}
                              value={line.quantity}
                              onChange={(e) =>
                                updateLine(line.product.id, { quantity: Number(e.target.value) })
                              }
                            />
                          </div>
                          <div>
                            <label className="text-xs text-muted-foreground">Giá nhập</label>
                            <Input
                              type="number"
                              min={0}
                              value={line.unitPrice}
                              onChange={(e) =>
                                updateLine(line.product.id, { unitPrice: Number(e.target.value) })
                              }
                            />
                          </div>
                        </div>
                        <div className="mt-2 flex justify-between text-sm text-muted-foreground">
                          <span>
                            Thành tiền:{" "}
                            <span className="font-medium text-foreground">
                              {numberFormatter.format(line.quantity * line.unitPrice)} đ
                            </span>
                          </span>
                          <span>
                            Giá vốn mới:{" "}
                            <span className="font-medium text-success">
                              {numberFormatter.format(newCost)} đ
                            </span>
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Nhà cung cấp</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <Select value={supplierId} onValueChange={setSupplierId}>
                <SelectTrigger>
                  <SelectValue placeholder="Chọn nhà cung cấp" />
                </SelectTrigger>
                <SelectContent>
                  {suppliersQuery.data?.data.map((s) => (
                    <SelectItem key={s.id} value={String(s.id)}>
                      {s.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {supplier && (
                <div className="text-sm">
                  {supplier.address && <div className="text-muted-foreground">{supplier.address}</div>}
                  <div className="mt-1 text-destructive">
                    Công nợ: <Money value={supplier.outstandingDebt} />
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Thanh toán</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Tổng tiền hàng</span>
                <Money value={totalAmount} className="font-medium" />
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="text-muted-foreground">Chiết khấu NCC</span>
                <Input
                  type="number"
                  min={0}
                  className="w-32 text-right"
                  value={discountAmount}
                  onChange={(e) => setDiscountAmount(Number(e.target.value))}
                />
              </div>
              <div className="flex items-center justify-between border-t pt-3 font-semibold">
                <span>Cần trả NCC</span>
                <Money value={payable} />
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="text-muted-foreground">Trả ngay</span>
                <Input
                  type="number"
                  min={0}
                  className="w-32 text-right"
                  value={paidAmount}
                  onChange={(e) => setPaidAmount(Number(e.target.value))}
                />
              </div>
              {debtAmount > 0 && (
                <div className="flex items-center justify-between rounded-md bg-warning/10 px-3 py-2 font-semibold text-warning">
                  <span>Ghi nợ NCC</span>
                  <Money value={debtAmount} />
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
