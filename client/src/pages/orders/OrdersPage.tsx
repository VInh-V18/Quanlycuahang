import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Download, MoreHorizontal, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { DateRangePicker } from "@/components/common/DateRangePicker";
import { Money } from "@/components/common/Money";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/use-toast";
import { cancelOrder, exportOrders, listOrders, type OrderListItem } from "@/lib/api/orders";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { useDebouncedValue } from "@/lib/hooks/useDebouncedValue";
import { getApiErrorMessage } from "@/lib/http/errors";
import { useAppSelector } from "@/store/hooks";

const PAGE_SIZE = 20;

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function OrderRowActions({ row }: { row: OrderListItem }) {
  const permissions = useAppSelector((state) => state.auth.permissions);
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);

  const cancelMutation = useMutation({
    mutationFn: () => cancelOrder(row.id),
    onSuccess: () => {
      toast({ title: "Đã hủy đơn hàng" });
      setCancelDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể hủy đơn", description: getApiErrorMessage(err) });
    },
  });

  const canCancel =
    permissions.includes("order:void") && (row.status === "completed" || row.status === "draft");

  return (
    <Dialog open={cancelDialogOpen} onOpenChange={setCancelDialogOpen}>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" className="h-8 w-8">
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem asChild>
            <Link to={`/orders/${row.id}`}>Xem chi tiết</Link>
          </DropdownMenuItem>
          <DropdownMenuItem
            asChild
            disabled={row.status === "cancelled" || row.status === "fully_returned"}
          >
            <Link to={`/orders/${row.id}/return`}>Tạo phiếu trả hàng</Link>
          </DropdownMenuItem>
          {canCancel && (
            <DropdownMenuItem
              className="text-destructive focus:text-destructive"
              onSelect={(e) => {
                e.preventDefault();
                setCancelDialogOpen(true);
              }}
            >
              Hủy đơn
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Xác nhận hủy đơn {row.orderNumber}?</DialogTitle>
          <DialogDescription>
            Hàng sẽ được hoàn lại vào tồn kho, công nợ liên quan (nếu chưa thu) sẽ được xóa. Chỉ hủy
            được đơn tạo trong ngày hôm nay.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => setCancelDialogOpen(false)}>
            Đóng
          </Button>
          <Button
            variant="destructive"
            disabled={cancelMutation.isPending}
            onClick={() => cancelMutation.mutate()}
          >
            Xác nhận hủy đơn
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function OrdersPage() {
  const branchId = useCurrentBranchId();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("all");
  const [range, setRange] = useState<{ from: Date; to: Date }>(() => {
    const today = new Date();
    return { from: today, to: today };
  });

  const debouncedSearch = useDebouncedValue(search);

  const params = useMemo(
    () => ({
      branchId,
      page,
      size: PAGE_SIZE,
      search: debouncedSearch,
      status: status === "all" ? undefined : status,
      from: toIsoDate(range.from),
      to: toIsoDate(range.to),
    }),
    [branchId, page, debouncedSearch, status, range],
  );

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["orders", params],
    queryFn: () => listOrders(params),
  });

  const todayTotal = (data?.data ?? []).reduce((sum, o) => sum + o.totalAmount, 0);

  const columns: DataTableColumn<OrderListItem>[] = [
    { key: "orderNumber", header: "Mã đơn" },
    {
      key: "createdAt",
      header: "Thời gian",
      render: (row) => new Date(row.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
    },
    {
      key: "customerName",
      header: "Khách hàng",
      render: (row) => (
        <div>
          <div>{row.customerName}</div>
          {row.customerPhone && <div className="text-xs text-muted-foreground">{row.customerPhone}</div>}
        </div>
      ),
    },
    { key: "cashierName", header: "Thu ngân" },
    {
      key: "totalAmount",
      header: "Tổng tiền",
      className: "text-right",
      render: (row) => <Money value={row.totalAmount} />,
    },
    {
      key: "paymentMethods",
      header: "Thanh toán",
      render: (row) =>
        row.hasDebt ? (
          <StatusBadge status="debt" />
        ) : row.paymentMethods.length > 0 ? (
          <span className="rounded-full bg-muted px-2.5 py-1 text-xs font-medium">
            {row.paymentMethods.join(" + ")}
          </span>
        ) : (
          <span className="text-muted-foreground">—</span>
        ),
    },
    {
      key: "status",
      header: "Trạng thái",
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      key: "id",
      header: "",
      render: (row) => <OrderRowActions row={row} />,
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Đơn hàng</h1>
          <p className="text-sm text-muted-foreground">
            {data?.meta?.total ?? 0} đơn · <Money value={todayTotal} />
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => exportOrders(params)}
          >
            <Download className="h-4 w-4" />
            Xuất Excel
          </Button>
          <Button asChild size="lg">
            <Link to="/pos">
              <Plus className="h-4 w-4" />
              Bán hàng (F1)
            </Link>
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Input
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          placeholder="Tìm mã đơn, tên khách, SĐT..."
          className="w-64"
        />
        <DateRangePicker
          value={range}
          onChange={(r) => r?.from && r?.to && setRange({ from: r.from, to: r.to })}
        />
        <Select
          value={status}
          onValueChange={(v) => {
            setStatus(v);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Trạng thái" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Trạng thái: Tất cả</SelectItem>
            <SelectItem value="completed">Hoàn thành</SelectItem>
            <SelectItem value="partially_returned">Trả một phần</SelectItem>
            <SelectItem value="fully_returned">Đã trả hết</SelectItem>
            <SelectItem value="cancelled">Đã hủy</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <DataTable
        columns={columns}
        data={data?.data ?? []}
        rowKey={(row) => row.id}
        meta={data?.meta}
        loading={isLoading}
        error={isError ? getApiErrorMessage(error) : null}
        onPageChange={setPage}
        emptyMessage="Không tìm thấy đơn hàng phù hợp"
      />
    </div>
  );
}
