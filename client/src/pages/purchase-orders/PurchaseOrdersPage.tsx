import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { Money } from "@/components/common/Money";
import { listPurchaseOrders, type PurchaseOrder } from "@/lib/api/purchaseOrders";
import { CURRENT_BRANCH_ID } from "@/lib/constants";
import { getApiErrorMessage } from "@/lib/http/errors";

export function PurchaseOrdersPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["purchase-orders", page],
    queryFn: () => listPurchaseOrders(CURRENT_BRANCH_ID, page),
  });

  const columns: DataTableColumn<PurchaseOrder>[] = [
    { key: "id", header: "Mã phiếu", render: (row) => `PN${String(row.id).padStart(6, "0")}` },
    { key: "supplierName", header: "Nhà cung cấp" },
    {
      key: "createdAt",
      header: "Ngày tạo",
      render: (row) => new Date(row.createdAt).toLocaleString("vi-VN"),
    },
    {
      key: "totalAmount",
      header: "Tổng tiền hàng",
      className: "text-right",
      render: (row) => <Money value={row.totalAmount} />,
    },
    {
      key: "discountAmount",
      header: "Chiết khấu",
      className: "text-right",
      render: (row) => <Money value={row.discountAmount} />,
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Nhập kho</h1>
          <p className="text-sm text-muted-foreground">Hàng hóa / Nhập kho</p>
        </div>
        <Button asChild size="lg">
          <Link to="/purchase-orders/new">
            <Plus className="h-4 w-4" />
            Tạo phiếu nhập
          </Link>
        </Button>
      </div>

      <DataTable
        columns={columns}
        data={data?.data ?? []}
        rowKey={(row) => row.id}
        meta={data?.meta}
        loading={isLoading}
        error={isError ? getApiErrorMessage(error) : null}
        onPageChange={setPage}
        emptyMessage="Chưa có phiếu nhập kho nào"
      />
    </div>
  );
}
