import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Download, MoreHorizontal, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
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
import { exportOrders, listOrders, type OrderListItem } from "@/lib/api/orders";
import { CURRENT_BRANCH_ID } from "@/lib/constants";
import { getApiErrorMessage } from "@/lib/http/errors";

const PAGE_SIZE = 20;

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function OrdersPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("all");
  const [range, setRange] = useState<{ from: Date; to: Date }>(() => {
    const today = new Date();
    return { from: today, to: today };
  });

  const params = useMemo(
    () => ({
      branchId: CURRENT_BRANCH_ID,
      page,
      size: PAGE_SIZE,
      search,
      status: status === "all" ? undefined : status,
      from: toIsoDate(range.from),
      to: toIsoDate(range.to),
    }),
    [page, search, status, range],
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
      render: (row) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="h-8 w-8">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem asChild disabled={row.status === "cancelled" || row.status === "fully_returned"}>
              <Link to={`/orders/${row.id}/return`}>Tạo phiếu trả hàng</Link>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
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
