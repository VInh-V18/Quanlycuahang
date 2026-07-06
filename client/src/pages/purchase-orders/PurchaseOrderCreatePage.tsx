import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Search, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Money } from "@/components/common/Money";
import { useToast } from "@/components/ui/use-toast";
import { createPurchaseOrder } from "@/lib/api/purchaseOrders";
import { searchProducts, type Product } from "@/lib/api/products";
import { listSuppliers } from "@/lib/api/suppliers";
import { CURRENT_BRANCH_ID } from "@/lib/constants";
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

export function PurchaseOrderCreatePage() {
  const [search, setSearch] = useState("");
  const [lines, setLines] = useState<DraftLine[]>([]);
  const [supplierId, setSupplierId] = useState<string>("");
  const [discountAmount, setDiscountAmount] = useState(0);
  const [paidAmount, setPaidAmount] = useState(0);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const searchQuery = useQuery({
    queryKey: ["products", "search-to-add", search],
    queryFn: () => searchProducts({ search, page: 0, size: 8, branchId: CURRENT_BRANCH_ID }),
    enabled: search.trim().length > 0,
  });

  const suppliersQuery = useQuery({ queryKey: ["suppliers"], queryFn: listSuppliers });
  const supplier = suppliersQuery.data?.data.find((s) => String(s.id) === supplierId);

  const totalAmount = useMemo(
    () => lines.reduce((sum, l) => sum + l.quantity * l.unitPrice, 0),
    [lines],
  );
  const payable = Math.max(0, totalAmount - discountAmount);
  const debtAmount = Math.max(0, payable - paidAmount);

  function addProduct(product: Product) {
    if (lines.some((l) => l.product.id === product.id)) return;
    setLines((prev) => [
      ...prev,
      { product, quantity: 1, unitPrice: product.costPrice ?? product.sellPrice, batchCode: "", expiryDate: "" },
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
        branchId: CURRENT_BRANCH_ID,
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
        <Button size="lg" disabled={!canSubmit} onClick={() => createMutation.mutate()}>
          Hoàn tất nhập kho
        </Button>
      </div>

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
                  <div className="absolute z-10 mt-1 w-full rounded-md border bg-popover shadow-md">
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
