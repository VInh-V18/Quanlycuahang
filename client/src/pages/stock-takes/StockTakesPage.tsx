import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Plus } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { createStockTake, listStockTakes, type StockTake } from "@/lib/api/stockTakes";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDateTime } from "@/lib/utils";
import { useToast } from "@/components/ui/use-toast";

/** Route /stock-takes/new — chi tao phieu kiem ke roi chuyen huong ngay, khong co UI rieng
 * (Link tu Ton kho tro thang vao day cho tien, giong "+Ban hang" tro thang vao POS). */
export function StockTakeNewPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const branchId = useCurrentBranchId();

  const createMutation = useMutation({
    mutationFn: () => createStockTake(branchId),
    onSuccess: (stockTake) => navigate(`/stock-takes/${stockTake.id}`, { replace: true }),
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể tạo phiếu", description: getApiErrorMessage(err) });
      navigate("/stock-takes", { replace: true });
    },
  });

  // useRef (khong phai chi useEffect([])) chan goi 2 lan - React 18 StrictMode (dev) mount/unmount/
  // remount component ngay lap tuc de bat loi side-effect, khien effect [] chay 2 LAN that su, va
  // vi than mutationFn nay la POST tao phieu kiem ke (co tac dung phu that, khong idempotent), chay
  // 2 lan tao ra 2 PHIEU KIEM KE TRUNG NHAU thay vi 1 (phat hien khi rieng soat).
  const requested = useRef(false);

  useEffect(() => {
    if (requested.current) return;
    requested.current = true;
    createMutation.mutate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <p className="text-sm text-muted-foreground">Đang tạo phiếu kiểm kê...</p>;
}

export function StockTakesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const branchId = useCurrentBranchId();
  const [page, setPage] = useState(0);

  // listStockTakes truoc day khong nhan tham so page (luon co dinh page=0/size=20), nen phieu kiem
  // ke thu 21 tro di khong co cach nao xem duoc du DataTable da san sang render pager - chi thieu
  // truyen onPageChange/page (phat hien khi rieng soat).
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["stock-takes", branchId, page],
    queryFn: () => listStockTakes(branchId, page),
  });

  const createMutation = useMutation({
    mutationFn: () => createStockTake(branchId),
    onSuccess: (stockTake) => {
      queryClient.invalidateQueries({ queryKey: ["stock-takes"] });
      navigate(`/stock-takes/${stockTake.id}`);
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể tạo phiếu", description: getApiErrorMessage(err) });
    },
  });

  const columns: DataTableColumn<StockTake>[] = [
    { key: "id", header: "Mã phiếu", render: (row) => `KK${String(row.id).padStart(6, "0")}` },
    {
      key: "createdAt",
      header: "Chốt tồn lúc",
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      key: "status",
      header: "Trạng thái",
      render: (row) => (
        <Badge variant={row.status === "approved" ? "success" : "warning"}>
          {row.status === "approved" ? "Đã duyệt" : "Đang kiểm"}
        </Badge>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Kiểm kê</h1>
          <p className="text-sm text-muted-foreground">Hàng hóa / Kiểm kê</p>
        </div>
        <Button size="lg" disabled={createMutation.isPending} onClick={() => createMutation.mutate()}>
          <Plus className="h-4 w-4" />
          Tạo phiếu kiểm kê
        </Button>
      </div>

      <DataTable
        columns={columns}
        data={data?.data ?? []}
        rowKey={(row) => row.id}
        meta={data?.meta}
        loading={isLoading}
        error={isError ? getApiErrorMessage(error) : null}
        emptyMessage="Chưa có phiếu kiểm kê nào"
        onRowClick={(row) => navigate(`/stock-takes/${row.id}`)}
        onPageChange={setPage}
      />
    </div>
  );
}
