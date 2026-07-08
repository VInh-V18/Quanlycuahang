import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
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
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Money } from "@/components/common/Money";
import { useToast } from "@/components/ui/use-toast";
import {
  getDebtAgingByPartner,
  getDebtHistory,
  getDebtSummary,
  recordDebtPayment,
  type DebtPartnerAging,
} from "@/lib/api/debts";
import { getApiErrorMessage } from "@/lib/http/errors";

const numberFormatter = new Intl.NumberFormat("vi-VN");

function PaymentDialog({
  direction,
  partner,
  onClose,
}: {
  direction: "receivable" | "payable";
  partner: DebtPartnerAging | null;
  onClose: () => void;
}) {
  const [partnerId, setPartnerId] = useState(partner ? String(partner.partnerId) : "");
  const [amount, setAmount] = useState(partner?.totalDebt ?? 0);
  const [method, setMethod] = useState("cash");
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const partnersQuery = useQuery({
    queryKey: ["debts", "by-partner", direction],
    queryFn: () => getDebtAgingByPartner(direction),
  });

  const mutation = useMutation({
    mutationFn: () =>
      recordDebtPayment({ direction, partnerId: Number(partnerId), amount, method, note }),
    onSuccess: () => {
      toast({ title: "Đã ghi nhận thanh toán" });
      queryClient.invalidateQueries({ queryKey: ["debts"] });
      onClose();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể ghi nhận", description: getApiErrorMessage(err) });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {direction === "receivable" ? "Ghi nhận thu nợ khách hàng" : "Ghi nhận trả nợ NCC"}
          </DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div>
            <Label>{direction === "receivable" ? "Khách hàng" : "Nhà cung cấp"}</Label>
            <Select value={partnerId} onValueChange={setPartnerId}>
              <SelectTrigger>
                <SelectValue placeholder="Chọn đối tác đang nợ" />
              </SelectTrigger>
              <SelectContent>
                {partnersQuery.data?.map((p) => (
                  <SelectItem key={p.partnerId} value={String(p.partnerId)}>
                    {p.partnerName} — {numberFormatter.format(p.totalDebt)}đ
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label htmlFor="payment-amount">Số tiền</Label>
            <Input
              id="payment-amount"
              type="number"
              value={amount}
              onChange={(e) => setAmount(Number(e.target.value))}
            />
          </div>
          <div>
            <Label>Hình thức</Label>
            <Select value={method} onValueChange={setMethod}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="cash">Tiền mặt</SelectItem>
                <SelectItem value="bank_transfer">Chuyển khoản</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label htmlFor="payment-note">Ghi chú</Label>
            <Input id="payment-note" value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={!partnerId || amount <= 0 || mutation.isPending} onClick={() => mutation.mutate()}>
            Xác nhận
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function DebtsPage() {
  const [direction, setDirection] = useState<"receivable" | "payable">("receivable");
  const [selectedPartner, setSelectedPartner] = useState<DebtPartnerAging | null>(null);
  const [showPayment, setShowPayment] = useState(false);
  const [agingFilter, setAgingFilter] = useState("all");

  const summaryQuery = useQuery({ queryKey: ["debts", "summary"], queryFn: getDebtSummary });

  const agingQuery = useQuery({
    queryKey: ["debts", "by-partner", direction],
    queryFn: () => getDebtAgingByPartner(direction),
  });

  const historyQuery = useQuery({
    queryKey: ["debts", "history", direction, selectedPartner?.partnerId],
    queryFn: () => getDebtHistory(selectedPartner!.partnerId, direction),
    enabled: !!selectedPartner,
  });

  const rows = useMemo(() => {
    let list = agingQuery.data ?? [];
    if (agingFilter === "0-7") list = list.filter((r) => r.bucket0to7 > 0);
    if (agingFilter === "8-30") list = list.filter((r) => r.bucket8to30 > 0);
    if (agingFilter === "30+") list = list.filter((r) => r.bucketOver30 > 0);
    return list;
  }, [agingQuery.data, agingFilter]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">Công nợ</h1>
        <Button size="lg" onClick={() => setShowPayment(true)}>
          <Plus className="h-4 w-4" />
          Ghi nhận thanh toán
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Phải thu khách hàng</CardDescription>
            <CardTitle className="text-2xl text-success">
              <Money value={summaryQuery.data?.receivableTotal ?? 0} />
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            {summaryQuery.data?.receivableCount ?? 0} khách
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Phải trả nhà cung cấp</CardDescription>
            <CardTitle className="text-2xl text-destructive">
              <Money value={summaryQuery.data?.payableTotal ?? 0} />
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            {summaryQuery.data?.payableCount ?? 0} NCC
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Nợ quá 30 ngày</CardDescription>
            <CardTitle className="text-2xl text-warning">
              <Money value={summaryQuery.data?.overdueReceivable ?? 0} />
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-warning">⚠ cần thu</CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <div className="flex items-center gap-3">
              <CardTitle>
                {direction === "receivable" ? "Phải thu khách hàng" : "Phải trả nhà cung cấp"}
              </CardTitle>
              <Tabs
                value={direction}
                onValueChange={(v) => {
                  setDirection(v as "receivable" | "payable");
                  setSelectedPartner(null);
                }}
              >
                <TabsList>
                  <TabsTrigger value="receivable">Khách hàng nợ</TabsTrigger>
                  <TabsTrigger value="payable">Phải trả NCC</TabsTrigger>
                </TabsList>
              </Tabs>
            </div>
            <Select value={agingFilter} onValueChange={setAgingFilter}>
              <SelectTrigger className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tuổi nợ: Tất cả</SelectItem>
                <SelectItem value="0-7">0-7 ngày</SelectItem>
                <SelectItem value="8-30">8-30 ngày</SelectItem>
                <SelectItem value="30+">&gt;30 ngày</SelectItem>
              </SelectContent>
            </Select>
          </CardHeader>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{direction === "receivable" ? "Khách hàng" : "Nhà cung cấp"}</TableHead>
                  <TableHead className="text-right">Dư nợ</TableHead>
                  <TableHead className="text-right">0-7 ngày</TableHead>
                  <TableHead className="text-right">8-30</TableHead>
                  <TableHead className="text-right">&gt;30</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((row) => (
                  <TableRow
                    key={row.partnerId}
                    className="cursor-pointer"
                    onClick={() => setSelectedPartner(row)}
                  >
                    <TableCell className="font-medium">{row.partnerName}</TableCell>
                    <TableCell className="text-right font-semibold text-destructive">
                      <Money value={row.totalDebt} />
                    </TableCell>
                    <TableCell className="text-right">{numberFormatter.format(row.bucket0to7)}</TableCell>
                    <TableCell className="text-right">{numberFormatter.format(row.bucket8to30)}</TableCell>
                    <TableCell className="text-right text-warning">
                      {numberFormatter.format(row.bucketOver30)}
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="link"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedPartner(row);
                          setShowPayment(true);
                        }}
                      >
                        {direction === "receivable" ? "Thu nợ" : "Trả nợ"}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
                {rows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} className="py-8 text-center text-muted-foreground">
                      {agingQuery.isLoading ? "Đang tải..." : "Không có công nợ"}
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
              {selectedPartner
                ? `Lịch sử đối chiếu — ${selectedPartner.partnerName}`
                : "Lịch sử đối chiếu"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {!selectedPartner ? (
              <p className="text-sm text-muted-foreground">Chọn 1 dòng ở bảng bên trái để xem</p>
            ) : (
              <div className="space-y-3">
                {historyQuery.data?.map((event, i) => (
                  <div key={i} className="flex items-center justify-between text-sm">
                    <div>
                      <span className="text-muted-foreground">
                        {new Date(event.eventAt).toLocaleDateString("vi-VN")} ·
                      </span>{" "}
                      {event.referenceCode && (
                        <span className="rounded bg-accent px-1.5 py-0.5 text-xs">{event.referenceCode}</span>
                      )}{" "}
                      {event.label}
                    </div>
                    <span className={event.amount >= 0 ? "text-destructive" : "text-success"}>
                      {event.amount >= 0 ? "+" : ""}
                      {numberFormatter.format(event.amount)}
                    </span>
                  </div>
                ))}
                <div className="flex items-center justify-between border-t pt-3 font-semibold">
                  <span>Dư nợ hiện tại</span>
                  <Money value={selectedPartner.totalDebt} />
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {showPayment && (
        <PaymentDialog
          direction={direction}
          partner={selectedPartner}
          onClose={() => setShowPayment(false)}
        />
      )}
    </div>
  );
}
