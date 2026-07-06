import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAppSelector } from "@/store/hooks";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useToast } from "@/components/ui/use-toast";
import { getSettings, listBranches, updateSettings } from "@/lib/api/settings";
import { getApiErrorMessage } from "@/lib/http/errors";

type FormState = Record<string, string>;

function useSettingsForm() {
  const query = useQuery({ queryKey: ["settings"], queryFn: getSettings });
  const [form, setForm] = useState<FormState>({});

  useEffect(() => {
    if (query.data) {
      setForm(query.data.settings);
    }
  }, [query.data]);

  return { query, form, setForm };
}

export function SettingsPage() {
  const [tab, setTab] = useState<"sales" | "invoice" | "store" | "security">("sales");
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const permissions = useAppSelector((state) => state.auth.permissions);
  const canEdit = permissions.includes("settings:update");

  const { query: settingsQuery, form, setForm } = useSettingsForm();

  const branchesQuery = useQuery({
    queryKey: ["branches"],
    queryFn: listBranches,
    enabled: tab === "store",
  });

  const saveMutation = useMutation({
    mutationFn: (keys: string[]) => {
      const payload: FormState = {};
      keys.forEach((k) => {
        payload[k] = form[k] ?? "";
      });
      return updateSettings(payload);
    },
    onSuccess: () => {
      toast({ title: "Đã lưu cài đặt" });
      queryClient.invalidateQueries({ queryKey: ["settings"] });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể lưu", description: getApiErrorMessage(err) });
    },
  });

  function set(key: string, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  const loading = settingsQuery.isLoading;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">Cài đặt</h1>
      </div>

      <Tabs value={tab} onValueChange={(v) => setTab(v as typeof tab)}>
        <TabsList>
          <TabsTrigger value="sales">Bán hàng & tiền tệ</TabsTrigger>
          <TabsTrigger value="invoice">Hóa đơn & thanh toán</TabsTrigger>
          <TabsTrigger value="store">Cửa hàng & chi nhánh</TabsTrigger>
          <TabsTrigger value="security">Bảo mật & hệ thống</TabsTrigger>
        </TabsList>
      </Tabs>

      {tab === "sales" && (
        <Card>
          <CardHeader>
            <CardTitle>Bán hàng & tiền tệ</CardTitle>
            <CardDescription>Áp dụng cho toàn bộ đơn hàng mới tạo tại POS</CardDescription>
          </CardHeader>
          <CardContent className="max-w-lg space-y-4">
            <div className="flex items-center justify-between rounded-md border p-3">
              <div>
                <Label className="mb-0">Giá sản phẩm đã gồm VAT</Label>
                <p className="text-xs text-muted-foreground">Mặc định khi tạo sản phẩm mới</p>
              </div>
              <Switch
                disabled={!canEdit || loading}
                checked={form.price_includes_vat_default === "true"}
                onCheckedChange={(checked) => set("price_includes_vat_default", String(checked))}
              />
            </div>
            <div className="flex items-center justify-between rounded-md border p-3">
              <div>
                <Label className="mb-0">Cho phép bán âm kho</Label>
                <p className="text-xs text-muted-foreground">Vẫn cho tạo đơn khi tồn kho không đủ</p>
              </div>
              <Switch
                disabled={!canEdit || loading}
                checked={form.allow_negative_stock === "true"}
                onCheckedChange={(checked) => set("allow_negative_stock", String(checked))}
              />
            </div>
            <div>
              <Label htmlFor="rounding-unit">Đơn vị làm tròn tổng tiền</Label>
              <Select
                disabled={!canEdit || loading}
                value={form.rounding_unit ?? "1000"}
                onValueChange={(v) => set("rounding_unit", v)}
              >
                <SelectTrigger id="rounding-unit">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">Không làm tròn</SelectItem>
                  <SelectItem value="100">100đ</SelectItem>
                  <SelectItem value="500">500đ</SelectItem>
                  <SelectItem value="1000">1.000đ</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label htmlFor="debt-limit-default">Hạn mức nợ mặc định cho khách hàng mới</Label>
              <Input
                id="debt-limit-default"
                type="number"
                disabled={!canEdit || loading}
                value={form.debt_limit_default ?? "0"}
                onChange={(e) => set("debt_limit_default", e.target.value)}
              />
            </div>
            {canEdit && (
              <Button
                disabled={loading || saveMutation.isPending}
                onClick={() =>
                  saveMutation.mutate([
                    "price_includes_vat_default",
                    "allow_negative_stock",
                    "rounding_unit",
                    "debt_limit_default",
                  ])
                }
              >
                Lưu thay đổi
              </Button>
            )}
          </CardContent>
        </Card>
      )}

      {tab === "invoice" && (
        <Card>
          <CardHeader>
            <CardTitle>Hóa đơn & thanh toán</CardTitle>
            <CardDescription>Tiền tố dùng để sinh mã tự động</CardDescription>
          </CardHeader>
          <CardContent className="max-w-lg space-y-4">
            <div>
              <Label htmlFor="order-prefix">Tiền tố mã đơn hàng</Label>
              <Input
                id="order-prefix"
                disabled={!canEdit || loading}
                value={form.order_number_prefix ?? ""}
                onChange={(e) => set("order_number_prefix", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="invoice-prefix">Tiền tố mã hóa đơn</Label>
              <Input
                id="invoice-prefix"
                disabled={!canEdit || loading}
                value={form.invoice_number_prefix ?? ""}
                onChange={(e) => set("invoice_number_prefix", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="sku-prefix">Tiền tố mã SKU sản phẩm</Label>
              <Input
                id="sku-prefix"
                disabled={!canEdit || loading}
                value={form.sku_prefix ?? ""}
                onChange={(e) => set("sku_prefix", e.target.value)}
              />
            </div>
            {canEdit && (
              <Button
                disabled={loading || saveMutation.isPending}
                onClick={() =>
                  saveMutation.mutate(["order_number_prefix", "invoice_number_prefix", "sku_prefix"])
                }
              >
                Lưu thay đổi
              </Button>
            )}
          </CardContent>
        </Card>
      )}

      {tab === "store" && (
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Thông tin cửa hàng</CardTitle>
              <CardDescription>Hiển thị trên hóa đơn in cho khách</CardDescription>
            </CardHeader>
            <CardContent className="max-w-lg space-y-4">
              <div>
                <Label htmlFor="store-name">Tên cửa hàng</Label>
                <Input
                  id="store-name"
                  disabled={!canEdit || loading}
                  value={form.store_name ?? ""}
                  onChange={(e) => set("store_name", e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="store-tax-code">Mã số thuế</Label>
                <Input
                  id="store-tax-code"
                  disabled={!canEdit || loading}
                  value={form.store_tax_code ?? ""}
                  onChange={(e) => set("store_tax_code", e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="store-address">Địa chỉ</Label>
                <Input
                  id="store-address"
                  disabled={!canEdit || loading}
                  value={form.store_address ?? ""}
                  onChange={(e) => set("store_address", e.target.value)}
                />
              </div>
              {canEdit && (
                <Button
                  disabled={loading || saveMutation.isPending}
                  onClick={() => saveMutation.mutate(["store_name", "store_tax_code", "store_address"])}
                >
                  Lưu thay đổi
                </Button>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Chi nhánh</CardTitle>
              <CardDescription>
                Hệ thống hiện quản lý 1 chi nhánh — thêm/sửa chi nhánh sẽ có ở phiên bản sau
              </CardDescription>
            </CardHeader>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Tên chi nhánh</TableHead>
                    <TableHead>Địa chỉ</TableHead>
                    <TableHead>Điện thoại</TableHead>
                    <TableHead>Trạng thái</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {branchesQuery.data?.map((b) => (
                    <TableRow key={b.id}>
                      <TableCell className="font-medium">{b.name}</TableCell>
                      <TableCell>{b.address ?? "—"}</TableCell>
                      <TableCell>{b.phone ?? "—"}</TableCell>
                      <TableCell>
                        {b.active ? (
                          <Badge variant="success">Đang hoạt động</Badge>
                        ) : (
                          <Badge variant="secondary">Ngừng hoạt động</Badge>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === "security" && (
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Bảo mật đăng nhập</CardTitle>
              <CardDescription>Chống dò mật khẩu bằng giới hạn số lần đăng nhập sai</CardDescription>
            </CardHeader>
            <CardContent className="max-w-lg space-y-4">
              <div>
                <Label htmlFor="login-rate-limit">Số lần đăng nhập sai tối đa (trong 15 phút)</Label>
                <Input
                  id="login-rate-limit"
                  type="number"
                  min={1}
                  disabled={!canEdit || loading}
                  value={form.login_rate_limit_attempts ?? "5"}
                  onChange={(e) => set("login_rate_limit_attempts", e.target.value)}
                />
                <p className="mt-1 text-xs text-muted-foreground">
                  Vượt quá số lần này, tài khoản bị khóa đăng nhập tạm thời trong 15 phút
                </p>
              </div>
              {canEdit && (
                <Button
                  disabled={loading || saveMutation.isPending}
                  onClick={() => saveMutation.mutate(["login_rate_limit_attempts"])}
                >
                  Lưu thay đổi
                </Button>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Thông tin phiên đăng nhập</CardTitle>
              <CardDescription>
                Cấu hình hệ thống (chỉ đọc) — thay đổi cần sửa cấu hình server và khởi động lại
              </CardDescription>
            </CardHeader>
            <CardContent className="grid max-w-lg grid-cols-2 gap-4">
              <div>
                <div className="text-xs text-muted-foreground">Access token hết hạn sau</div>
                <div className="text-lg font-semibold">
                  {settingsQuery.data?.systemInfo.accessTokenTtlMinutes ?? "—"} phút
                </div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Refresh token hết hạn sau</div>
                <div className="text-lg font-semibold">
                  {settingsQuery.data?.systemInfo.refreshTokenTtlDays ?? "—"} ngày
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
