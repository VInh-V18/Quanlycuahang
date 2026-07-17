import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { Pencil } from "lucide-react";
import { PermissionGate } from "@/components/common/PermissionGate";
import { QueryBoundary } from "@/components/common/QueryBoundary";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NumberInput } from "@/components/common/NumberInput";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useToast } from "@/components/ui/use-toast";
import { Money } from "@/components/common/Money";
import {
  getPurchaseOrder,
  updatePurchaseOrderItemPrice,
  type PurchaseOrderItem,
} from "@/lib/api/purchaseOrders";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDateTime } from "@/lib/utils";

const numberFormatter = new Intl.NumberFormat("vi-VN");

function EditPriceDialog({
  item,
  onClose,
}: {
  item: PurchaseOrderItem;
  onClose: () => void;
}) {
  const [newPrice, setNewPrice] = useState(item.unitPrice);
  const [reason, setReason] = useState("");
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const delta = (newPrice - item.unitPrice) * item.quantity;
  const canSave = newPrice > 0 && reason.trim().length > 0;

  const mutation = useMutation({
    mutationFn: () => updatePurchaseOrderItemPrice(item.id, newPrice, reason.trim()),
    onSuccess: () => {
      toast({ title: "Đã sửa giá nhập" });
      queryClient.invalidateQueries({ queryKey: ["purchase-orders"] });
      queryClient.invalidateQueries({ queryKey: ["debts"] });
      onClose();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể sửa giá", description: getApiErrorMessage(err) });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Sửa giá nhập — {item.productName}</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div>
            <Label htmlFor="edit-price-value">Giá nhập đúng</Label>
            <NumberInput
              id="edit-price-value"
              min={0}
              value={newPrice}
              onValueChange={(v) => setNewPrice(v ?? 0)}
            />
            <p className="mt-1 text-xs text-muted-foreground">
              Giá cũ: {numberFormatter.format(item.unitPrice)}đ (đã nhập {numberFormatter.format(item.quantity)} đơn vị)
            </p>
          </div>
          <div>
            <Label htmlFor="edit-price-reason">Lý do sửa (bắt buộc)</Label>
            <Input
              id="edit-price-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="VD: Nhập nhầm 15.000 thành 150.000"
            />
          </div>
          {delta !== 0 && (
            <p className={`text-sm font-medium ${delta > 0 ? "text-destructive" : "text-success"}`}>
              Tổng tiền phiếu sẽ {delta > 0 ? "tăng" : "giảm"} {numberFormatter.format(Math.abs(delta))}đ
              — công nợ nhà cung cấp (nếu có) sẽ tự điều chỉnh theo.
            </p>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={!canSave || mutation.isPending} onClick={() => mutation.mutate()}>
            Lưu
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function PurchaseOrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const purchaseOrderId = Number(id);
  const [editingItem, setEditingItem] = useState<PurchaseOrderItem | null>(null);

  const {
    data: purchaseOrder,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["purchase-orders", purchaseOrderId],
    queryFn: () => getPurchaseOrder(purchaseOrderId),
  });

  return (
    <QueryBoundary
      isLoading={isLoading}
      isError={isError}
      error={error}
      data={purchaseOrder}
      onRetry={() => refetch()}
      notFoundMessage="Không tìm thấy phiếu nhập này — có thể đã bị xoá hoặc bạn không có quyền xem."
    >
      {(purchaseOrder) => (
        <div className="space-y-4">
          <div>
            <h1 className="text-2xl font-bold">
              Phiếu nhập{" "}
              <span className="text-muted-foreground">
                PN{String(purchaseOrder.id).padStart(6, "0")}
              </span>
            </h1>
            <p className="text-sm text-muted-foreground">
              {purchaseOrder.supplierName} · {formatDateTime(purchaseOrder.createdAt)}
            </p>
          </div>

          <Card>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Sản phẩm</TableHead>
                    <TableHead className="text-right">Số lượng</TableHead>
                    <TableHead className="text-right">Đơn giá nhập</TableHead>
                    <TableHead className="text-right">Thành tiền</TableHead>
                    <TableHead className="w-10" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {purchaseOrder.items.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.productName}</TableCell>
                      <TableCell className="text-right">
                        {numberFormatter.format(item.quantity)}
                      </TableCell>
                      <TableCell className="text-right">
                        {numberFormatter.format(item.unitPrice)}đ
                      </TableCell>
                      <TableCell className="text-right">
                        <Money value={item.unitPrice * item.quantity} />
                      </TableCell>
                      <TableCell>
                        <PermissionGate perm="purchase-order:update">
                          <Button
                            variant="ghost"
                            size="icon"
                            title="Sửa giá nhập (nhập sai)"
                            onClick={() => setEditingItem(item)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                        </PermissionGate>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <div className="ml-auto max-w-xs space-y-1 text-sm">
            <div className="flex justify-between text-muted-foreground">
              <span>Tổng tiền hàng</span>
              <Money value={purchaseOrder.totalAmount} />
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>Chiết khấu</span>
              <Money value={purchaseOrder.discountAmount} />
            </div>
            <div className="flex justify-between border-t pt-1 text-base font-semibold">
              <span>Cần trả NCC</span>
              <Money value={purchaseOrder.totalAmount - purchaseOrder.discountAmount} />
            </div>
          </div>

          {editingItem && <EditPriceDialog item={editingItem} onClose={() => setEditingItem(null)} />}
        </div>
      )}
    </QueryBoundary>
  );
}
