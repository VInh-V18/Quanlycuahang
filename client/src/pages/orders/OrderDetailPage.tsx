import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { NumberInput } from "@/components/common/NumberInput";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Money } from "@/components/common/Money";
import { PermissionGate } from "@/components/common/PermissionGate";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/use-toast";
import { searchProducts, type Product } from "@/lib/api/products";
import { cancelOrder, editOrder, getOrder, type EditOrderLine } from "@/lib/api/orders";
import { useCurrentBranchId } from "@/lib/hooks/useCurrentBranchId";
import { useDebouncedValue } from "@/lib/hooks/useDebouncedValue";
import { getApiErrorMessage } from "@/lib/http/errors";
import { formatDateTime } from "@/lib/utils";
import { useAppSelector } from "@/store/hooks";

const numberFormatter = new Intl.NumberFormat("vi-VN");

interface EditableLine {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineDiscountAmount: number;
}

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const branchId = useCurrentBranchId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const permissions = useAppSelector((state) => state.auth.permissions);

  const [editing, setEditing] = useState(false);
  const [lines, setLines] = useState<EditableLine[]>([]);
  const [productSearch, setProductSearch] = useState("");
  const debouncedProductSearch = useDebouncedValue(productSearch);
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);

  const { data: order, isLoading } = useQuery({
    queryKey: ["orders", orderId],
    queryFn: () => getOrder(orderId),
  });

  const { data: productResults } = useQuery({
    queryKey: ["products", "search-for-edit", branchId, debouncedProductSearch],
    queryFn: () => searchProducts({ branchId, search: debouncedProductSearch, size: 8 }),
    enabled: editing && debouncedProductSearch.trim().length > 0,
  });

  function startEditing() {
    if (!order) return;
    setLines(
      order.items.map((item) => ({
        productId: item.productId,
        productName: item.productName,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        lineDiscountAmount: item.discountAmount,
      })),
    );
    setEditing(true);
  }

  function cancelEditing() {
    setEditing(false);
    setLines([]);
    setProductSearch("");
  }

  function updateLine(index: number, patch: Partial<EditableLine>) {
    setLines((prev) => prev.map((line, i) => (i === index ? { ...line, ...patch } : line)));
  }

  function removeLine(index: number) {
    setLines((prev) => prev.filter((_, i) => i !== index));
  }

  function addProduct(product: Product) {
    if (lines.some((line) => line.productId === product.id)) {
      toast({
        variant: "destructive",
        title: "Sản phẩm đã có trong đơn",
        description: "Sửa số lượng ở dòng hiện có thay vì thêm dòng mới.",
      });
      return;
    }
    setLines((prev) => [
      ...prev,
      {
        productId: product.id,
        productName: product.name,
        quantity: 1,
        unitPrice: product.sellPrice,
        lineDiscountAmount: 0,
      },
    ]);
    setProductSearch("");
  }

  const previewTotal = lines.reduce(
    (sum, line) => sum + line.quantity * line.unitPrice - line.lineDiscountAmount,
    0,
  );

  const editMutation = useMutation({
    mutationFn: () => {
      const payload: EditOrderLine[] = lines.map((line) => ({
        productId: line.productId,
        quantity: line.quantity,
        unitPrice: line.unitPrice,
        lineDiscountAmount: line.lineDiscountAmount,
      }));
      return editOrder(orderId, { lines: payload });
    },
    onSuccess: () => {
      toast({ title: "Đã lưu thay đổi đơn hàng" });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      cancelEditing();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể sửa đơn", description: getApiErrorMessage(err) });
    },
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelOrder(orderId),
    onSuccess: () => {
      toast({ title: "Đã hủy đơn hàng" });
      setCancelDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể hủy đơn", description: getApiErrorMessage(err) });
    },
  });

  useEffect(() => {
    if (editing) return;
    setProductSearch("");
  }, [editing]);

  if (isLoading || !order) {
    return <p className="text-sm text-muted-foreground">Đang tải...</p>;
  }

  const canEditThisOrder = permissions.includes("order:edit") && order.status === "completed";
  const canCancelThisOrder =
    permissions.includes("order:void") &&
    (order.status === "completed" || order.status === "draft");

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold">Đơn hàng {order.orderNumber}</h1>
            <StatusBadge status={order.status} />
          </div>
          <p className="text-sm text-muted-foreground">
            Tạo lúc {formatDateTime(order.createdAt)}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => navigate("/orders")}>
            Quay lại
          </Button>
          {!editing && (
            <PermissionGate perm="return:create">
              <Button variant="outline" asChild>
                <Link to={`/orders/${order.id}/return`}>Tạo phiếu trả hàng</Link>
              </Button>
            </PermissionGate>
          )}
          {!editing && canCancelThisOrder && (
            <Dialog open={cancelDialogOpen} onOpenChange={setCancelDialogOpen}>
              <DialogTrigger asChild>
                <Button variant="destructive">Hủy đơn</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Xác nhận hủy đơn {order.orderNumber}?</DialogTitle>
                  <DialogDescription>
                    Hàng sẽ được hoàn lại vào tồn kho, công nợ liên quan (nếu chưa thu) sẽ được xóa.
                    Chỉ hủy được đơn tạo trong ngày hôm nay. Hành động này không thể hoàn tác qua giao
                    diện.
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
          )}
          {!editing && canEditThisOrder && (
            <Button onClick={startEditing}>Sửa đơn</Button>
          )}
          {editing && (
            <>
              <Button variant="outline" onClick={cancelEditing} disabled={editMutation.isPending}>
                Hủy sửa
              </Button>
              <Button
                disabled={lines.length === 0 || editMutation.isPending}
                onClick={() => editMutation.mutate()}
              >
                Lưu thay đổi
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="space-y-3 p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Sản phẩm</TableHead>
                  <TableHead className="text-right">Số lượng</TableHead>
                  <TableHead className="text-right">Đơn giá</TableHead>
                  <TableHead className="text-right">Giảm giá</TableHead>
                  <TableHead className="text-right">Thành tiền</TableHead>
                  {editing && <TableHead className="w-10" />}
                </TableRow>
              </TableHeader>
              <TableBody>
                {editing
                  ? lines.map((line, index) => (
                      <TableRow key={line.productId}>
                        <TableCell className="font-medium">{line.productName}</TableCell>
                        <TableCell className="text-right">
                          <NumberInput
                            min={0.01}
                            value={line.quantity}
                            onValueChange={(v) => updateLine(index, { quantity: v ?? 0 })}
                            className="ml-auto w-24 text-right"
                          />
                        </TableCell>
                        <TableCell className="text-right">
                          <NumberInput
                            allowDecimal={false}
                            min={0}
                            value={line.unitPrice}
                            onValueChange={(v) => updateLine(index, { unitPrice: v ?? 0 })}
                            className="ml-auto w-28 text-right"
                          />
                        </TableCell>
                        <TableCell className="text-right">
                          <NumberInput
                            allowDecimal={false}
                            min={0}
                            value={line.lineDiscountAmount}
                            onValueChange={(v) =>
                              updateLine(index, { lineDiscountAmount: v ?? 0 })
                            }
                            className="ml-auto w-24 text-right"
                          />
                        </TableCell>
                        <TableCell className="text-right">
                          {numberFormatter.format(
                            line.quantity * line.unitPrice - line.lineDiscountAmount,
                          )}
                        </TableCell>
                        <TableCell>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-destructive"
                            onClick={() => removeLine(index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  : order.items.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell className="font-medium">{item.productName}</TableCell>
                        <TableCell className="text-right">
                          {numberFormatter.format(item.quantity)}
                        </TableCell>
                        <TableCell className="text-right">
                          <Money value={item.unitPrice} />
                        </TableCell>
                        <TableCell className="text-right">
                          <Money value={item.discountAmount} />
                        </TableCell>
                        <TableCell className="text-right font-medium">
                          <Money value={item.lineTotal} />
                        </TableCell>
                      </TableRow>
                    ))}
              </TableBody>
            </Table>

            {editing && (
              <div className="space-y-2 p-4 pt-0">
                <label className="text-sm text-muted-foreground">Thêm sản phẩm</label>
                <div className="relative">
                  <Input
                    value={productSearch}
                    onChange={(e) => setProductSearch(e.target.value)}
                    placeholder="Tìm tên/SKU sản phẩm để thêm vào đơn..."
                  />
                  {productResults && productResults.data.length > 0 && (
                    <div className="absolute z-10 mt-1 w-full rounded-md border bg-popover shadow-md">
                      {productResults.data.map((product) => (
                        <button
                          key={product.id}
                          type="button"
                          className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-accent"
                          onClick={() => addProduct(product)}
                        >
                          <span>{product.name}</span>
                          <span className="text-muted-foreground">
                            <Money value={product.sellPrice} />
                          </span>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  Tổng tiền tạm tính: {numberFormatter.format(previewTotal)} đ — số tiền chính xác
                  (thuế/làm tròn) do server tính lại sau khi lưu.
                </p>
              </div>
            )}

            {!editing && (
              <p className="p-4 text-xs text-muted-foreground">
                * Sửa đơn (số lượng/sản phẩm/đơn giá/giảm giá) chỉ dành cho Chủ cửa hàng/Quản lý,
                không giới hạn thời gian. Hủy đơn chỉ áp dụng cho đơn tạo trong ngày.
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Tổng quan</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">Tạm tính</span>
              <Money value={order.subtotalAmount} />
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">Giảm giá</span>
              <Money value={order.discountAmount} />
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">Thuế VAT</span>
              <Money value={order.vatAmount} />
            </div>
            <div className="flex items-center justify-between border-t pt-3">
              <span className="text-sm font-semibold">Tổng cộng</span>
              <span className="text-xl font-bold">
                <Money value={order.totalAmount} />
              </span>
            </div>
            {order.invoiceNumber && (
              <div className="flex items-center justify-between text-sm text-muted-foreground">
                <span>Hóa đơn</span>
                <span>{order.invoiceNumber}</span>
              </div>
            )}
            {order.note && (
              <div className="text-sm text-muted-foreground">
                <span className="font-medium">Ghi chú: </span>
                {order.note}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
