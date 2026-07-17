import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { NumberInput } from "@/components/common/NumberInput";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
import { getOrder } from "@/lib/api/orders";
import { createReturn } from "@/lib/api/returns";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDateTime } from "@/lib/utils";

const numberFormatter = new Intl.NumberFormat("vi-VN");

const REFUND_METHODS = [
  { value: "cash", label: "Tiền mặt (gắn ca hiện tại)" },
  { value: "bank_transfer", label: "Chuyển khoản" },
];

const RETURN_REASONS = [
  "Trái bị dập / hư hỏng",
  "Giao nhầm sản phẩm",
  "Khách đổi ý",
  "Hàng cận/quá hạn sử dụng",
  "Khác",
];

export function ReturnCreatePage() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [quantities, setQuantities] = useState<Record<number, string>>({});
  const [refundMethod, setRefundMethod] = useState("cash");
  const [reason, setReason] = useState(RETURN_REASONS[0]);

  const { data: order, isLoading } = useQuery({
    queryKey: ["orders", orderId],
    queryFn: () => getOrder(orderId),
  });

  const lines = useMemo(() => {
    if (!order) return [];
    return order.items.map((item) => {
      const remaining = item.quantity - item.returnedQuantity;
      const unitEffectivePrice = item.lineTotal / item.quantity;
      const qtyRaw = quantities[item.id] ?? "0";
      const qty = Math.min(Number(qtyRaw) || 0, remaining);
      const refund = Math.round(unitEffectivePrice * qty);
      return { item, remaining, unitEffectivePrice, qty, refund };
    });
  }, [order, quantities]);

  const totalRefund = lines.reduce((sum, l) => sum + l.refund, 0);

  const returnMutation = useMutation({
    mutationFn: () =>
      createReturn({
        orderId,
        refundMethod,
        items: lines
          .filter((l) => l.qty > 0)
          .map((l) => ({ orderItemId: l.item.id, quantity: l.qty })),
      }),
    onSuccess: () => {
      toast({ title: "Đã xác nhận trả hàng & hoàn tiền" });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      navigate("/orders");
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể trả hàng", description: getApiErrorMessage(err) });
    },
  });

  if (isLoading || !order) {
    return <p className="text-sm text-muted-foreground">Đang tải...</p>;
  }

  const canSubmit = lines.some((l) => l.qty > 0) && !returnMutation.isPending;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Tạo phiếu trả hàng</h1>
          <p className="text-sm text-muted-foreground">
            Từ hóa đơn {order.orderNumber} · {formatDateTime(order.createdAt)}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => navigate("/orders")}>
            Hủy
          </Button>
          <Button variant="destructive" disabled={!canSubmit} onClick={() => returnMutation.mutate()}>
            Xác nhận trả &amp; hoàn tiền
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Sản phẩm</TableHead>
                  <TableHead className="text-right">Đã mua</TableHead>
                  <TableHead className="text-right">Đã trả</TableHead>
                  <TableHead className="text-right">Trả lần này</TableHead>
                  <TableHead className="text-right">Đơn giá thực trả</TableHead>
                  <TableHead className="text-right">Hoàn tiền</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {lines.map(({ item, remaining, unitEffectivePrice, refund }) => (
                  <TableRow key={item.id}>
                    <TableCell className="font-medium">{item.productName}</TableCell>
                    <TableCell className="text-right">{numberFormatter.format(item.quantity)}</TableCell>
                    <TableCell className="text-right">{numberFormatter.format(item.returnedQuantity)}</TableCell>
                    <TableCell className="text-right">
                      <NumberInput
                        min={0}
                        disabled={remaining <= 0}
                        value={quantities[item.id] ?? ""}
                        onValueChange={(v) =>
                          setQuantities((prev) => ({ ...prev, [item.id]: v == null ? "" : String(v) }))
                        }
                        placeholder={remaining <= 0 ? "đã trả đủ" : "0"}
                        className="ml-auto w-24 text-right"
                      />
                    </TableCell>
                    <TableCell className="text-right">
                      {numberFormatter.format(Math.round(unitEffectivePrice))}
                    </TableCell>
                    <TableCell className="text-right font-medium text-destructive">
                      {numberFormatter.format(refund)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <p className="p-4 text-xs text-muted-foreground">
              * Đơn giá thực trả = đơn giá sau mọi chiết khấu đã phân bổ về dòng. Hàng nhập lại kho
              với giá vốn snapshot lúc bán.
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Hoàn tiền</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">Tổng hoàn khách</span>
              <span className="text-xl font-bold text-destructive">
                <Money value={totalRefund} />
              </span>
            </div>
            <div>
              <label className="text-sm text-muted-foreground">Hình thức hoàn</label>
              <Select value={refundMethod} onValueChange={setRefundMethod}>
                <SelectTrigger className="mt-1">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {REFUND_METHODS.map((m) => (
                    <SelectItem key={m.value} value={m.value}>
                      {m.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-sm text-muted-foreground">Lý do trả</label>
              <Select value={reason} onValueChange={setReason}>
                <SelectTrigger className="mt-1">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {RETURN_REASONS.map((r) => (
                    <SelectItem key={r} value={r}>
                      {r}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
