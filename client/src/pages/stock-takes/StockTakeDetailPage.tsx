import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { QueryBoundary } from "@/components/common/QueryBoundary";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
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
import { useToast } from "@/components/ui/use-toast";
import {
  approveStockTake,
  DISCREPANCY_REASONS,
  getStockTake,
  submitCounts,
  type StockTakeItem,
} from "@/lib/api/stockTakes";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDateTime } from "@/lib/utils";

const numberFormatter = new Intl.NumberFormat("vi-VN");

interface LocalCount {
  actualQty: string;
  reason: string;
}

export function StockTakeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const stockTakeId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [counts, setCounts] = useState<Record<number, LocalCount>>({});
  const [onlyDiscrepancy, setOnlyDiscrepancy] = useState(false);
  const [search, setSearch] = useState("");

  const {
    data: stockTake,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["stock-takes", stockTakeId],
    queryFn: () => getStockTake(stockTakeId),
  });

  const isDraft = stockTake?.status === "draft";

  useEffect(() => {
    if (!stockTake) return;
    setCounts((prev) => {
      const next = { ...prev };
      for (const item of stockTake.items) {
        if (!(item.id in next)) {
          next[item.id] = {
            actualQty: item.actualQty != null ? String(item.actualQty) : "",
            reason: item.reason ?? "",
          };
        }
      }
      return next;
    });
  }, [stockTake]);

  const saveMutation = useMutation({
    mutationFn: () => {
      const items = Object.entries(counts)
        .filter(([, v]) => v.actualQty !== "")
        .map(([itemId, v]) => ({
          stockTakeItemId: Number(itemId),
          actualQty: Number(v.actualQty),
          reason: v.reason || undefined,
        }));
      return submitCounts(stockTakeId, items);
    },
    onSuccess: () => {
      toast({ title: "Đã lưu tạm" });
      queryClient.invalidateQueries({ queryKey: ["stock-takes", stockTakeId] });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể lưu", description: getApiErrorMessage(err) });
    },
  });

  const approveMutation = useMutation({
    mutationFn: async () => {
      await saveMutation.mutateAsync();
      return approveStockTake(stockTakeId);
    },
    onSuccess: () => {
      toast({ title: "Đã hoàn tất & cân bằng kho" });
      queryClient.invalidateQueries({ queryKey: ["stock-takes"] });
      navigate("/stock-takes");
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể duyệt", description: getApiErrorMessage(err) });
    },
  });

  function diffOf(item: StockTakeItem): number | null {
    const raw = counts[item.id]?.actualQty;
    if (raw === undefined || raw === "") return null;
    return Number(raw) - item.expectedQty;
  }

  const countedCount = useMemo(
    () => Object.values(counts).filter((c) => c.actualQty !== "").length,
    [counts],
  );

  const { increaseQty, decreaseQty } = useMemo(() => {
    let increase = 0;
    let decrease = 0;
    for (const item of stockTake?.items ?? []) {
      const diff = diffOf(item);
      if (diff == null) continue;
      if (diff > 0) increase += diff;
      if (diff < 0) decrease += diff;
    }
    return { increaseQty: increase, decreaseQty: decrease };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stockTake, counts]);

  const visibleItems = useMemo(() => {
    let items = stockTake?.items ?? [];
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      items = items.filter((i) => i.productName.toLowerCase().includes(q));
    }
    if (onlyDiscrepancy) {
      items = items.filter((i) => {
        const diff = diffOf(i);
        return diff != null && diff !== 0;
      });
    }
    return items;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stockTake, search, onlyDiscrepancy, counts]);

  return (
    <QueryBoundary
      isLoading={isLoading}
      isError={isError}
      error={error}
      data={stockTake}
      onRetry={() => refetch()}
      notFoundMessage="Không tìm thấy phiếu kiểm kê này — có thể đã bị xoá hoặc bạn không có quyền xem."
    >
      {(stockTake) => (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div>
            <h1 className="text-2xl font-bold">
              Kiểm kê{" "}
              <span className="text-muted-foreground">
                KK{String(stockTake.id).padStart(6, "0")}
              </span>
            </h1>
            <p className="text-sm text-muted-foreground">
              Chốt tồn lúc {formatDateTime(stockTake.createdAt)}
            </p>
          </div>
          <Badge variant={isDraft ? "warning" : "success"}>
            {isDraft ? "Đang kiểm" : "Đã duyệt"}
          </Badge>
        </div>
        {isDraft && (
          <div className="flex gap-2">
            <Button variant="outline" disabled={saveMutation.isPending} onClick={() => saveMutation.mutate()}>
              Lưu tạm
            </Button>
            <Button
              size="lg"
              disabled={approveMutation.isPending}
              onClick={() => approveMutation.mutate()}
            >
              Hoàn tất &amp; cân bằng kho
            </Button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Sản phẩm đã cân/đếm</CardDescription>
            <CardTitle className="text-2xl">
              {countedCount} / {stockTake.items.length}
            </CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Lệch tăng</CardDescription>
            <CardTitle className="text-2xl text-success">
              +{numberFormatter.format(increaseQty)}
            </CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Lệch giảm</CardDescription>
            <CardTitle className="text-2xl text-destructive">
              {numberFormatter.format(decreaseQty)}
            </CardTitle>
          </CardHeader>
        </Card>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Tìm sản phẩm..."
          className="w-64"
        />
        <div className="flex items-center gap-2 rounded-md border px-3 py-2">
          <Switch checked={onlyDiscrepancy} onCheckedChange={setOnlyDiscrepancy} id="only-discrepancy" />
          <label htmlFor="only-discrepancy" className="text-sm">
            Chỉ hiện dòng lệch
          </label>
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Sản phẩm</TableHead>
                <TableHead className="text-right">Tồn hệ thống (chốt)</TableHead>
                <TableHead className="text-right">Thực cân/đếm</TableHead>
                <TableHead className="text-right">Lệch</TableHead>
                <TableHead>Lý do</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {visibleItems.map((item) => {
                const diff = diffOf(item);
                return (
                  <TableRow key={item.id}>
                    <TableCell className="font-medium">{item.productName}</TableCell>
                    <TableCell className="text-right">
                      {numberFormatter.format(item.expectedQty)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Input
                        type="number"
                        disabled={!isDraft}
                        value={counts[item.id]?.actualQty ?? ""}
                        onChange={(e) =>
                          setCounts((prev) => ({
                            ...prev,
                            [item.id]: { ...prev[item.id], actualQty: e.target.value },
                          }))
                        }
                        placeholder="Chưa cân"
                        className="ml-auto w-28 text-right"
                      />
                    </TableCell>
                    <TableCell
                      className={`text-right font-medium ${diff && diff !== 0 ? (diff > 0 ? "text-success" : "text-destructive") : ""}`}
                    >
                      {diff == null ? "—" : `${diff > 0 ? "+" : ""}${numberFormatter.format(diff)}`}
                    </TableCell>
                    <TableCell>
                      {diff && diff !== 0 ? (
                        <Select
                          value={counts[item.id]?.reason ?? ""}
                          onValueChange={(v) =>
                            setCounts((prev) => ({
                              ...prev,
                              [item.id]: { ...prev[item.id], reason: v },
                            }))
                          }
                          disabled={!isDraft}
                        >
                          <SelectTrigger className="w-48">
                            <SelectValue placeholder="Chọn lý do" />
                          </SelectTrigger>
                          <SelectContent>
                            {DISCREPANCY_REASONS.map((r) => (
                              <SelectItem key={r} value={r}>
                                {r}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
      )}
    </QueryBoundary>
  );
}
