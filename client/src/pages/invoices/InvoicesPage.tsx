import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Printer } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { DateRangePicker } from "@/components/common/DateRangePicker";
import { Money } from "@/components/common/Money";
import { listInvoices, type InvoiceListItem } from "@/lib/api/invoices";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { getApiErrorMessage } from "@/lib/http/errors";

const PAGE_SIZE = 20;

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function InvoicesPage() {
  const branchId = useCurrentBranchId();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [range, setRange] = useState<{ from: Date; to: Date }>(() => {
    const today = new Date();
    return { from: today, to: today };
  });

  const params = useMemo(
    () => ({
      branchId,
      page,
      size: PAGE_SIZE,
      search,
      from: toIsoDate(range.from),
      to: toIsoDate(range.to),
    }),
    [branchId, page, search, range],
  );

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["invoices", params],
    queryFn: () => listInvoices(params),
  });

  const todayTotal = (data?.data ?? []).reduce((sum, i) => sum + i.totalAmount, 0);

  const columns: DataTableColumn<InvoiceListItem>[] = [
    { key: "invoiceNumber", header: "Số hóa đơn" },
    {
      key: "issuedAt",
      header: "Thời gian",
      render: (row) =>
        new Date(row.issuedAt).toLocaleString("vi-VN", {
          day: "2-digit",
          month: "2-digit",
          hour: "2-digit",
          minute: "2-digit",
        }),
    },
    { key: "orderNumber", header: "Mã đơn" },
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
    {
      key: "totalAmount",
      header: "Tổng tiền",
      className: "text-right",
      render: (row) => <Money value={row.totalAmount} />,
    },
    {
      key: "id",
      header: "",
      render: (row) => (
        <Button variant="ghost" size="sm" asChild>
          <Link to={`/invoices/${row.id}/print`}>
            <Printer className="h-4 w-4" />
            Xem/in
          </Link>
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Hóa đơn</h1>
          <p className="text-sm text-muted-foreground">
            {data?.meta?.total ?? 0} hóa đơn · <Money value={todayTotal} />
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Input
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          placeholder="Tìm số hóa đơn, mã đơn, tên khách, SĐT..."
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
        emptyMessage="Không tìm thấy hóa đơn phù hợp"
      />
    </div>
  );
}
