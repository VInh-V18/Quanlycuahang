import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Minus, Wallet } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { Money } from "@/components/common/Money";
import { QueryBoundary } from "@/components/common/QueryBoundary";
import { useToast } from "@/components/ui/use-toast";
import {
  addCashTransaction,
  closeShift,
  getCurrentShift,
  getShiftById,
  listShiftHistory,
  openShift,
  type ShiftDetail,
  type ShiftSummary,
} from "@/lib/api/shifts";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDateTime as formatDateTimeShared } from "@/lib/utils";

const PAGE_SIZE = 20;

function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  return formatDateTimeShared(iso);
}

function OpenShiftCard() {
  const [openingCash, setOpeningCash] = useState(0);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => openShift(openingCash, note || undefined),
    onSuccess: () => {
      toast({ title: "Đã mở ca" });
      queryClient.invalidateQueries({ queryKey: ["shifts"] });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể mở ca", description: getApiErrorMessage(err) });
    },
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Mở ca làm việc</CardTitle>
        <CardDescription>Nhập số tiền mặt có sẵn trong két trước khi bắt đầu bán hàng</CardDescription>
      </CardHeader>
      <CardContent className="max-w-sm space-y-3">
        <div>
          <Label htmlFor="opening-cash">Tiền mặt đầu ca</Label>
          <Input
            id="opening-cash"
            type="number"
            value={openingCash}
            onChange={(e) => setOpeningCash(Number(e.target.value))}
          />
        </div>
        <div>
          <Label htmlFor="open-note">Ghi chú</Label>
          <Input id="open-note" value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
        <Button
          size="lg"
          disabled={openingCash < 0 || mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          <Wallet className="h-4 w-4" />
          Mở ca
        </Button>
      </CardContent>
    </Card>
  );
}

function CashTransactionDialog({ shiftId, onClose }: { shiftId: number; onClose: () => void }) {
  const [type, setType] = useState<"cash_in" | "cash_out">("cash_in");
  const [amount, setAmount] = useState(0);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => addCashTransaction(shiftId, type, amount, note || undefined),
    onSuccess: () => {
      toast({ title: "Đã ghi giao dịch tiền mặt" });
      queryClient.invalidateQueries({ queryKey: ["shifts"] });
      onClose();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể ghi", description: getApiErrorMessage(err) });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Ghi thu/chi tiền mặt ngoài đơn</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div>
            <Label>Loại giao dịch</Label>
            <Select value={type} onValueChange={(v) => setType(v as "cash_in" | "cash_out")}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="cash_in">Thu tiền mặt</SelectItem>
                <SelectItem value="cash_out">Chi tiền mặt</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label htmlFor="tx-amount">Số tiền</Label>
            <Input
              id="tx-amount"
              type="number"
              value={amount}
              onChange={(e) => setAmount(Number(e.target.value))}
            />
          </div>
          <div>
            <Label htmlFor="tx-note">Ghi chú</Label>
            <Input id="tx-note" value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={amount <= 0 || mutation.isPending} onClick={() => mutation.mutate()}>
            Xác nhận
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function CloseShiftDialog({ shift, onClose }: { shift: ShiftDetail; onClose: () => void }) {
  const [actualCash, setActualCash] = useState(shift.expectedCash);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const previewDiscrepancy = actualCash - shift.expectedCash;

  const mutation = useMutation({
    mutationFn: () => closeShift(shift.id, actualCash, note || undefined),
    onSuccess: () => {
      toast({ title: "Đã đóng ca" });
      queryClient.invalidateQueries({ queryKey: ["shifts"] });
      onClose();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể đóng ca", description: getApiErrorMessage(err) });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Đóng ca & đối chiếu tiền</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div className="rounded-md border p-3 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Tiền mặt dự kiến trong két</span>
              <span className="font-semibold">
                <Money value={shift.expectedCash} />
              </span>
            </div>
          </div>
          <div>
            <Label htmlFor="actual-cash">Tiền mặt thực đếm được</Label>
            <Input
              id="actual-cash"
              type="number"
              value={actualCash}
              onChange={(e) => setActualCash(Number(e.target.value))}
            />
          </div>
          <div className="flex items-center justify-between rounded-md border p-3 text-sm">
            <span className="text-muted-foreground">Chênh lệch</span>
            <span
              className={
                previewDiscrepancy === 0
                  ? "font-semibold text-success"
                  : "font-semibold text-destructive"
              }
            >
              {previewDiscrepancy > 0 ? "+" : ""}
              {previewDiscrepancy.toLocaleString("vi-VN")}đ
            </span>
          </div>
          <div>
            <Label htmlFor="close-note">Ghi chú</Label>
            <Input id="close-note" value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button
            variant="destructive"
            disabled={actualCash < 0 || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            Xác nhận đóng ca
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/** Lưới thống kê + danh sách thu/chi trong ca — dùng chung cho "Ca đang mở" (CurrentShiftCard) VÀ
 * dialog xem lại chi tiết 1 ca đã đóng trong lịch sử (ShiftDetailDialog), tránh 2 bản lệch nhau. */
function ShiftDetailStats({ shift }: { shift: ShiftDetail }) {
  return (
    <>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <div>
          <div className="text-xs text-muted-foreground">Tiền đầu ca</div>
          <div className="text-lg font-semibold">
            <Money value={shift.openingCash} />
          </div>
        </div>
        <div>
          <div className="text-xs text-muted-foreground">Bán tiền mặt</div>
          <div className="text-lg font-semibold text-success">
            <Money value={shift.cashSalesTotal} />
          </div>
        </div>
        <div>
          <div className="text-xs text-muted-foreground">Chuyển khoản / Thẻ</div>
          <div className="text-lg font-semibold">
            <Money value={shift.bankTransferSalesTotal + shift.cardSalesTotal} />
          </div>
        </div>
        <div>
          <div className="text-xs text-muted-foreground">Số đơn</div>
          <div className="text-lg font-semibold">{shift.orderCount}</div>
        </div>
        <div>
          <div className="text-xs text-muted-foreground">Thu tiền mặt ngoài đơn</div>
          <div className="text-lg font-semibold text-success">
            <Money value={shift.cashInTotal} />
          </div>
        </div>
        <div>
          <div className="text-xs text-muted-foreground">Chi tiền mặt ngoài đơn</div>
          <div className="text-lg font-semibold text-destructive">
            <Money value={shift.cashOutTotal} />
          </div>
        </div>
        <div>
          <div className="text-xs text-muted-foreground">Hoàn tiền mặt (trả hàng)</div>
          <div className="text-lg font-semibold text-destructive">
            <Money value={shift.cashRefundTotal} />
          </div>
        </div>
        <div>
          <div className="text-xs text-muted-foreground">Tiền mặt dự kiến trong két</div>
          <div className="text-lg font-bold text-primary">
            <Money value={shift.expectedCash} />
          </div>
        </div>
        {shift.status === "closed" && (
          <div>
            <div className="text-xs text-muted-foreground">Tiền mặt thực đếm</div>
            <div
              className={`text-lg font-bold ${
                shift.discrepancy === 0 ? "text-success" : "text-destructive"
              }`}
            >
              <Money value={shift.actualCash ?? 0} />
            </div>
          </div>
        )}
        {shift.status === "closed" && (
          <div>
            <div className="text-xs text-muted-foreground">Chênh lệch</div>
            <div
              className={`text-lg font-bold ${
                shift.discrepancy === 0 ? "text-success" : "text-destructive"
              }`}
            >
              {shift.discrepancy != null && shift.discrepancy > 0 ? "+" : ""}
              {(shift.discrepancy ?? 0).toLocaleString("vi-VN")}đ
            </div>
          </div>
        )}
      </div>
      {shift.cashTransactions.length > 0 && (
        <div className="mt-4 border-t pt-3">
          <div className="mb-2 text-sm font-semibold">Giao dịch thu/chi trong ca</div>
          <div className="space-y-2">
            {shift.cashTransactions.map((tx) => (
              <div key={tx.id} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  {tx.type === "cash_in" ? (
                    <Plus className="h-3.5 w-3.5 text-success" />
                  ) : (
                    <Minus className="h-3.5 w-3.5 text-destructive" />
                  )}
                  <span>{tx.note || (tx.type === "cash_in" ? "Thu tiền mặt" : "Chi tiền mặt")}</span>
                  <span className="text-xs text-muted-foreground">{formatDateTime(tx.createdAt)}</span>
                </div>
                <span className={tx.type === "cash_in" ? "text-success" : "text-destructive"}>
                  {tx.type === "cash_in" ? "+" : "-"}
                  <Money value={tx.amount} />
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </>
  );
}

function CurrentShiftCard({ shift }: { shift: ShiftDetail }) {
  const [showCashTx, setShowCashTx] = useState(false);
  const [showClose, setShowClose] = useState(false);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <div>
          <CardTitle>Ca đang mở</CardTitle>
          <CardDescription>
            Mở lúc {formatDateTime(shift.openedAt)} · {shift.cashierName}
          </CardDescription>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setShowCashTx(true)}>
            <Plus className="h-4 w-4" />
            Thu/chi tiền mặt
          </Button>
          <Button variant="destructive" onClick={() => setShowClose(true)}>
            Đóng ca
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <ShiftDetailStats shift={shift} />
      </CardContent>

      {showCashTx && <CashTransactionDialog shiftId={shift.id} onClose={() => setShowCashTx(false)} />}
      {showClose && <CloseShiftDialog shift={shift} onClose={() => setShowClose(false)} />}
    </Card>
  );
}

/** Xem lại chi tiết 1 ca TỪ LỊCH SỬ (đang mở hoặc đã đóng) — chọn dialog thay vì trang/route riêng
 * vì đây chỉ là xem lại (read-only), không có hành động sửa nào cần URL riêng để chia sẻ/bookmark;
 * dialog nhẹ hơn, giữ nguyên ngữ cảnh danh sách lịch sử ca đang xem (roadmap Prompt #5, mục 2). */
function ShiftDetailDialog({ shiftId, onClose }: { shiftId: number; onClose: () => void }) {
  const {
    data: shift,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["shifts", "detail", shiftId],
    queryFn: () => getShiftById(shiftId),
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Chi tiết ca làm việc</DialogTitle>
        </DialogHeader>
        <QueryBoundary
          isLoading={isLoading}
          isError={isError}
          error={error}
          data={shift}
          onRetry={() => refetch()}
          notFoundMessage="Không tìm thấy ca làm việc này — có thể đã bị xoá hoặc bạn không có quyền xem."
        >
          {(shift) => (
            <div>
              <p className="mb-3 text-sm text-muted-foreground">
                Mở lúc {formatDateTime(shift.openedAt)}
                {shift.closedAt && <> · Đóng lúc {formatDateTime(shift.closedAt)}</>} ·{" "}
                {shift.cashierName}
              </p>
              <ShiftDetailStats shift={shift} />
            </div>
          )}
        </QueryBoundary>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Đóng
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function ShiftsPage() {
  const [statusFilter, setStatusFilter] = useState("all");
  const [page, setPage] = useState(0);
  const [selectedShiftId, setSelectedShiftId] = useState<number | null>(null);

  const currentShiftQuery = useQuery({ queryKey: ["shifts", "current"], queryFn: getCurrentShift });

  const historyParams = useMemo(
    () => ({
      status: statusFilter === "all" ? undefined : statusFilter,
      page,
      size: PAGE_SIZE,
    }),
    [statusFilter, page],
  );

  const historyQuery = useQuery({
    queryKey: ["shifts", "history", historyParams],
    queryFn: () => listShiftHistory(historyParams),
  });

  const columns: DataTableColumn<ShiftSummary>[] = [
    { key: "openedAt", header: "Mở ca", render: (row) => formatDateTime(row.openedAt) },
    { key: "closedAt", header: "Đóng ca", render: (row) => formatDateTime(row.closedAt) },
    { key: "cashierName", header: "Thu ngân" },
    {
      key: "openingCash",
      header: "Tiền đầu ca",
      className: "text-right",
      render: (row) => <Money value={row.openingCash} />,
    },
    {
      key: "actualCash",
      header: "Tiền thực tế",
      className: "text-right",
      render: (row) => (row.actualCash != null ? <Money value={row.actualCash} /> : "—"),
    },
    {
      key: "discrepancy",
      header: "Chênh lệch",
      className: "text-right",
      render: (row) =>
        row.discrepancy != null ? (
          <span className={row.discrepancy === 0 ? "text-success" : "font-semibold text-destructive"}>
            {row.discrepancy > 0 ? "+" : ""}
            {row.discrepancy.toLocaleString("vi-VN")}đ
          </span>
        ) : (
          "—"
        ),
    },
    {
      key: "status",
      header: "Trạng thái",
      render: (row) =>
        row.status === "open" ? (
          <Badge variant="success">Đang mở</Badge>
        ) : (
          <Badge variant="secondary">Đã đóng</Badge>
        ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">Ca & két tiền</h1>
      </div>

      {currentShiftQuery.isLoading ? null : currentShiftQuery.data ? (
        <CurrentShiftCard shift={currentShiftQuery.data} />
      ) : (
        <OpenShiftCard />
      )}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle>Lịch sử ca</CardTitle>
          <Select
            value={statusFilter}
            onValueChange={(v) => {
              setStatusFilter(v);
              setPage(0);
            }}
          >
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Trạng thái: Tất cả</SelectItem>
              <SelectItem value="open">Đang mở</SelectItem>
              <SelectItem value="closed">Đã đóng</SelectItem>
            </SelectContent>
          </Select>
        </CardHeader>
        <CardContent className="p-0">
          <DataTable
            columns={columns}
            data={historyQuery.data?.data ?? []}
            rowKey={(row) => row.id}
            meta={historyQuery.data?.meta}
            loading={historyQuery.isLoading}
            error={historyQuery.isError ? getApiErrorMessage(historyQuery.error) : null}
            onPageChange={setPage}
            onRowClick={(row) => setSelectedShiftId(row.id)}
            emptyMessage="Chưa có ca làm việc nào"
          />
        </CardContent>
      </Card>

      {selectedShiftId != null && (
        <ShiftDetailDialog shiftId={selectedShiftId} onClose={() => setSelectedShiftId(null)} />
      )}
    </div>
  );
}
