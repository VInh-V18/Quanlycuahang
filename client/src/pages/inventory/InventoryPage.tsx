import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Download, Plus } from "lucide-react";
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
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Money } from "@/components/common/Money";
import {
  exportInventory,
  getInventoryTransactions,
  listInventory,
  listLowStock,
  TRANSACTION_TYPE_LABELS,
  type InventoryItem,
} from "@/lib/api/inventory";
import { getInventoryValue } from "@/lib/api/reports";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";

const numberFormatter = new Intl.NumberFormat("vi-VN");

const REFERENCE_PREFIX: Record<string, string> = {
  order: "HD",
  purchase_order: "PN",
  return: "TH",
  stock_take: "KK",
};

function referenceCode(type: string | null, id: number | null): string {
  if (!type || !id) return "—";
  const prefix = REFERENCE_PREFIX[type] ?? type;
  return `${prefix}${String(id).padStart(6, "0")}`;
}

function daysUntil(isoDate: string): number {
  const diffMs = new Date(isoDate + "T00:00:00").getTime() - new Date().setHours(0, 0, 0, 0);
  return Math.round(diffMs / 86_400_000);
}

function statusOf(row: InventoryItem, expiryThreshold: number) {
  if (row.nearestExpiryDate && daysUntil(row.nearestExpiryDate) <= expiryThreshold) {
    return { label: "Cận hạn — xả 30%", className: "bg-destructive/10 text-destructive" };
  }
  if (row.stock <= row.minStock) {
    return { label: "Dưới định mức", className: "bg-warning/10 text-warning" };
  }
  return { label: "Đủ hàng", className: "bg-success/10 text-success" };
}

export function InventoryPage() {
  const branchId = useCurrentBranchId();
  const [search, setSearch] = useState("");
  const [onlyLowStock, setOnlyLowStock] = useState(false);
  const [expiryFilter, setExpiryFilter] = useState("all");
  const [selectedProduct, setSelectedProduct] = useState<InventoryItem | null>(null);

  const inventoryQuery = useQuery({
    queryKey: ["inventory", branchId, onlyLowStock],
    queryFn: () => (onlyLowStock ? listLowStock(branchId) : listInventory(branchId)),
  });

  const valueQuery = useQuery({
    queryKey: ["reports", "inventory-value", branchId],
    queryFn: () => getInventoryValue({ branchId, groupBy: "branch" }),
  });

  const transactionsQuery = useQuery({
    queryKey: ["inventory", "transactions", selectedProduct?.productId, branchId],
    queryFn: () => getInventoryTransactions(selectedProduct!.productId, branchId),
    enabled: !!selectedProduct,
  });

  const expiryThresholds: Record<string, number> = { "3": 3, "7": 7, "30": 30 };

  const rows = useMemo(() => {
    let list = inventoryQuery.data?.data ?? [];
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      list = list.filter(
        (r) => r.productName.toLowerCase().includes(q) || r.sku.toLowerCase().includes(q),
      );
    }
    if (expiryFilter !== "all") {
      const threshold = expiryThresholds[expiryFilter];
      list = list.filter((r) => r.nearestExpiryDate && daysUntil(r.nearestExpiryDate) <= threshold);
    }
    return list;
  }, [inventoryQuery.data, search, expiryFilter]);

  const totalValue = valueQuery.data?.[0]?.totalValue ?? 0;

  const ledger = useMemo(() => {
    const txs = transactionsQuery.data ?? [];
    if (!selectedProduct) return [];
    let balance = selectedProduct.stock;
    return txs.map((tx) => {
      const row = { ...tx, balanceAfter: balance };
      balance -= tx.quantity;
      return row;
    });
  }, [transactionsQuery.data, selectedProduct]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Tồn kho</h1>
          <p className="text-sm text-muted-foreground">
            Giá trị tồn theo giá vốn: <Money value={totalValue} className="font-medium text-foreground" />
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => exportInventory(branchId, onlyLowStock)}
          >
            <Download className="h-4 w-4" />
            Xuất Excel
          </Button>
          <Button asChild size="lg">
            <Link to="/stock-takes/new">
              <Plus className="h-4 w-4" />
              Tạo phiếu kiểm kê
            </Link>
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Tìm sản phẩm..."
          className="w-56"
        />
        <div className="flex items-center gap-2 rounded-md border px-3 py-2">
          <Switch checked={onlyLowStock} onCheckedChange={setOnlyLowStock} id="only-low-stock" />
          <label htmlFor="only-low-stock" className="text-sm">
            Chỉ hàng dưới định mức
          </label>
        </div>
        <Select value={expiryFilter} onValueChange={setExpiryFilter}>
          <SelectTrigger className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Cận HSD: Tất cả</SelectItem>
            <SelectItem value="3">Cận HSD ≤ 3 ngày</SelectItem>
            <SelectItem value="7">Cận HSD ≤ 7 ngày</SelectItem>
            <SelectItem value="30">Cận HSD ≤ 30 ngày</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Sản phẩm</TableHead>
                  <TableHead className="text-right">Tồn</TableHead>
                  <TableHead className="text-right">Tối thiểu</TableHead>
                  <TableHead>Lô / HSD</TableHead>
                  <TableHead className="text-right">Giá trị tồn</TableHead>
                  <TableHead>Trạng thái</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((row) => {
                  const status = statusOf(row, 7);
                  return (
                    <TableRow
                      key={row.id}
                      className="cursor-pointer"
                      onClick={() => setSelectedProduct(row)}
                    >
                      <TableCell>
                        <div className="font-medium">{row.productName}</div>
                        <div className="text-xs text-muted-foreground">{row.sku}</div>
                      </TableCell>
                      <TableCell className="text-right">{numberFormatter.format(row.stock)}</TableCell>
                      <TableCell className="text-right text-muted-foreground">
                        {numberFormatter.format(row.minStock)}
                      </TableCell>
                      <TableCell>
                        {row.nearestBatchCode ? (
                          <span className="text-sm">
                            {row.nearestBatchCode}
                            {row.nearestExpiryDate && ` · HSD ${row.nearestExpiryDate.split("-").reverse().join("/")}`}
                          </span>
                        ) : (
                          <span className="text-muted-foreground">—</span>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        <Money value={row.stock * row.costPrice} />
                      </TableCell>
                      <TableCell>
                        <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${status.className}`}>
                          {status.label}
                        </span>
                      </TableCell>
                    </TableRow>
                  );
                })}
                {rows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} className="py-8 text-center text-muted-foreground">
                      {inventoryQuery.isLoading ? "Đang tải..." : "Không có sản phẩm phù hợp"}
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>
              {selectedProduct ? `Thẻ kho — ${selectedProduct.productName}` : "Thẻ kho"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {!selectedProduct ? (
              <p className="text-sm text-muted-foreground">Chọn 1 sản phẩm ở bảng bên trái để xem</p>
            ) : ledger.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                {transactionsQuery.isLoading ? "Đang tải..." : "Chưa có giao dịch"}
              </p>
            ) : (
              <div className="space-y-3">
                {ledger.map((tx) => (
                  <div key={tx.id} className="flex items-center justify-between text-sm">
                    <div>
                      <div className="text-muted-foreground">
                        {new Date(tx.createdAt).toLocaleString("vi-VN")}
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="rounded bg-accent px-1.5 py-0.5 text-xs font-medium">
                          {TRANSACTION_TYPE_LABELS[tx.type] ?? tx.type}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          {referenceCode(tx.referenceType, tx.referenceId)}
                        </span>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className={tx.quantity >= 0 ? "text-success" : "text-destructive"}>
                        {tx.quantity >= 0 ? "+" : ""}
                        {numberFormatter.format(tx.quantity)}
                      </div>
                      <div className="text-xs text-muted-foreground">
                        Tồn {numberFormatter.format(tx.balanceAfter)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
