import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { DateRangePicker } from "@/components/common/DateRangePicker";
import { Money } from "@/components/common/Money";
import { useAppSelector } from "@/store/hooks";
import { listReturns, type ReturnListItem } from "@/lib/api/returns";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { useDebouncedValue } from "@/lib/hooks/useDebouncedValue";
import { getApiErrorMessage } from "@/lib/http/errors";

const PAGE_SIZE = 20;

const REFUND_METHOD_LABELS: Record<string, string> = {
  cash: "Tiền mặt",
  bank_transfer: "Chuyển khoản",
};

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/** Lịch sử phiếu trả hàng đã tạo (nav "Trả hàng") — tạo phiếu trả mới thực hiện từ trang Đơn hàng
 * (chọn đúng đơn cần trả qua menu "..."), trang này chỉ để xem lại các phiếu đã trả. */
export function ReturnsSearchPage() {
  const permissions = useAppSelector((state) => state.auth.permissions);
  const canCreate = permissions.includes("return:create");
  const branchId = useCurrentBranchId();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [range, setRange] = useState<{ from: Date; to: Date }>(() => {
    const today = new Date();
    const from = new Date(today);
    from.setDate(from.getDate() - 30);
    return { from, to: today };
  });

  const debouncedSearch = useDebouncedValue(search);

  const params = useMemo(
    () => ({
      branchId,
      page,
      size: PAGE_SIZE,
      search: debouncedSearch,
      from: toIsoDate(range.from),
      to: toIsoDate(range.to),
    }),
    [branchId, page, debouncedSearch, range],
  );

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["returns", params],
    queryFn: () => listReturns(params),
  });

  const totalRefund = (data?.data ?? []).reduce((sum, r) => sum + r.totalRefund, 0);

  const columns: DataTableColumn<ReturnListItem>[] = [
    {
      key: "createdAt",
      header: "Ngày trả",
      render: (row) =>
        new Date(row.createdAt).toLocaleString("vi-VN", {
          day: "2-digit",
          month: "2-digit",
          year: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        }),
    },
    { key: "orderNumber", header: "Đơn gốc" },
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
    { key: "itemCount", header: "Số dòng trả", className: "text-right" },
    {
      key: "totalRefund",
      header: "Số tiền hoàn",
      className: "text-right",
      render: (row) => <Money value={row.totalRefund} />,
    },
    {
      key: "refundMethod",
      header: "Hình thức hoàn",
      render: (row) => (
        <span className="rounded-full bg-muted px-2.5 py-1 text-xs font-medium">
          {REFUND_METHOD_LABELS[row.refundMethod] ?? row.refundMethod}
        </span>
      ),
    },
    { key: "createdByName", header: "Người tạo" },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Trả hàng</h1>
          <p className="text-sm text-muted-foreground">
            {data?.meta?.total ?? 0} phiếu trả · <Money value={totalRefund} /> đã hoàn
          </p>
        </div>
        {canCreate && (
          <Button asChild size="lg">
            <Link to="/orders">
              <Plus className="h-4 w-4" />
              Tạo phiếu trả hàng
            </Link>
          </Button>
        )}
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
      </div>

      <DataTable
        columns={columns}
        data={data?.data ?? []}
        rowKey={(row) => row.id}
        meta={data?.meta}
        loading={isLoading}
        error={isError ? getApiErrorMessage(error) : null}
        onPageChange={setPage}
        emptyMessage="Chưa có phiếu trả hàng nào trong khoảng thời gian này"
      />
    </div>
  );
}
