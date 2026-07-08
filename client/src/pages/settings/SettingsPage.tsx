import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2, X } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { useAppSelector } from "@/store/hooks";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
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
import {
  createBranch,
  deleteBranch,
  getSettings,
  listBranches,
  updateBranch,
  updateSettings,
  type Branch,
} from "@/lib/api/settings";
import { apiClient } from "@/lib/http/apiClient";
import { getApiErrorMessage } from "@/lib/http/errors";
import { decodeQrFromFile } from "@/lib/qrImageDecoder";
import { generateVietQrPayload, parseVietQrPayload, VIETQR_BANKS } from "@/lib/vietqr";

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
  const canManageBranch = permissions.includes("branch:manage");
  const [uploadingQr, setUploadingQr] = useState(false);
  const [addingBranch, setAddingBranch] = useState(false);

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
      queryClient.invalidateQueries({ queryKey: ["branding"] });
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể lưu", description: getApiErrorMessage(err) });
    },
  });

  function set(key: string, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleQrUpload(file: File) {
    setUploadingQr(true);
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await apiClient.post<{ data: { url: string } }>("/uploads", body);
      set("bank_qr_image_url", response.data.data.url);
    } catch (err) {
      toast({ variant: "destructive", title: "Tải ảnh thất bại", description: getApiErrorMessage(err) });
      setUploadingQr(false);
      return;
    }

    // Doc thu QR trong anh vua upload de tu dien so TK/ngan hang/chu TK — best-effort, khong chan
    // luong upload: anh mo hoac khong phai VietQR thi bo qua, nguoi dung van tu nhap tay duoc.
    try {
      const payload = await decodeQrFromFile(file);
      if (payload) {
        const parsed = parseVietQrPayload(payload);
        if (parsed.bankBin || parsed.accountNumber || parsed.merchantName) {
          if (parsed.bankBin) {
            set("bank_bin", parsed.bankBin);
            const bank = VIETQR_BANKS.find((b) => b.bin === parsed.bankBin);
            if (bank) set("bank_name", bank.shortName);
          }
          if (parsed.accountNumber) set("bank_account_number", parsed.accountNumber);
          if (parsed.merchantName) set("bank_account_name", parsed.merchantName);
          toast({ title: "Đã tự động điền thông tin ngân hàng từ ảnh QR" });
        }
      }
    } catch {
      // Khong doc duoc QR trong anh — im lang, khong phai loi can bao.
    } finally {
      setUploadingQr(false);
    }
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
        <div className="space-y-3">
          <Card>
            <CardHeader>
              <CardTitle>Cửa hàng & tài khoản nhận tiền</CardTitle>
              <CardDescription>Hiển thị trên hóa đơn in cho khách</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <div>
                  <Label htmlFor="store-name">Tên cửa hàng</Label>
                  <Input
                    id="store-name"
                    disabled={!canEdit || loading}
                    value={form.store_name ?? ""}
                    onChange={(e) => set("store_name", e.target.value)}
                    placeholder="Hiển thị ở góc trái menu và trang đăng nhập"
                  />
                </div>
                <div>
                  <Label htmlFor="store-slogan">Khẩu hiệu (tuỳ chọn)</Label>
                  <Input
                    id="store-slogan"
                    disabled={!canEdit || loading}
                    value={form.store_slogan ?? ""}
                    onChange={(e) => set("store_slogan", e.target.value)}
                    placeholder="VD: Chuỗi cửa hàng trái cây"
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
                <div>
                  <Label htmlFor="store-phone">Số điện thoại</Label>
                  <Input
                    id="store-phone"
                    disabled={!canEdit || loading}
                    value={form.store_phone ?? ""}
                    onChange={(e) => set("store_phone", e.target.value)}
                    placeholder="VD: 0396302983 - 0354639686"
                  />
                </div>
              </div>

              <div className="grid gap-3 border-t pt-4 sm:grid-cols-2">
                <div>
                  <Label htmlFor="bank-account-name">Chủ tài khoản</Label>
                  <Input
                    id="bank-account-name"
                    disabled={!canEdit || loading}
                    value={form.bank_account_name ?? ""}
                    onChange={(e) => set("bank_account_name", e.target.value.toUpperCase())}
                    placeholder="VD: TRINH THI LIEN"
                  />
                </div>
                <div>
                  <Label htmlFor="bank-account-number">Số tài khoản</Label>
                  <Input
                    id="bank-account-number"
                    disabled={!canEdit || loading}
                    value={form.bank_account_number ?? ""}
                    onChange={(e) => set("bank_account_number", e.target.value.trim())}
                  />
                </div>
                <div>
                  <Label htmlFor="bank-name">Ngân hàng</Label>
                  <Select
                    disabled={!canEdit || loading}
                    value={form.bank_bin ?? ""}
                    onValueChange={(bin) => {
                      const bank = VIETQR_BANKS.find((b) => b.bin === bin);
                      set("bank_bin", bin);
                      set("bank_name", bank?.shortName ?? "");
                    }}
                  >
                    <SelectTrigger id="bank-name">
                      <SelectValue placeholder="Chọn ngân hàng" />
                    </SelectTrigger>
                    <SelectContent>
                      {VIETQR_BANKS.map((bank) => (
                        <SelectItem key={bank.bin} value={bank.bin}>
                          {bank.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label>Ảnh QR dự phòng (khi chưa chọn ngân hàng ở trên)</Label>
                  <div className="mt-1 flex items-center gap-3">
                    {form.bank_qr_image_url && (
                      <img
                        src={form.bank_qr_image_url}
                        alt="QR nhận chuyển khoản"
                        className="h-14 w-14 rounded-md border object-cover"
                      />
                    )}
                    {canEdit && (
                      <label className="flex h-14 flex-1 cursor-pointer items-center justify-center gap-1 rounded-md border border-dashed text-center text-xs text-muted-foreground hover:bg-accent">
                        <span>{uploadingQr ? "Đang tải..." : "Bấm để tải ảnh QR (JPG/PNG, tối đa 2MB)"}</span>
                        <input
                          type="file"
                          accept="image/jpeg,image/png,image/webp"
                          className="hidden"
                          onChange={(e) => e.target.files?.[0] && handleQrUpload(e.target.files[0])}
                        />
                      </label>
                    )}
                  </div>
                </div>
              </div>

              <QrPreview bankBin={form.bank_bin} accountNumber={form.bank_account_number} accountName={form.bank_account_name} />

              {canEdit && (
                <Button
                  disabled={loading || saveMutation.isPending}
                  onClick={() =>
                    saveMutation.mutate([
                      "store_name",
                      "store_slogan",
                      "store_tax_code",
                      "store_address",
                      "store_phone",
                      "bank_account_name",
                      "bank_account_number",
                      "bank_name",
                      "bank_bin",
                      "bank_qr_image_url",
                    ])
                  }
                >
                  Lưu thay đổi
                </Button>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 py-4">
              <div>
                <CardTitle>Chi nhánh</CardTitle>
                <CardDescription>Danh sách chi nhánh đang hoạt động</CardDescription>
              </div>
              {canManageBranch && !addingBranch && (
                <Button size="sm" onClick={() => setAddingBranch(true)}>
                  <Plus className="h-4 w-4" />
                  Thêm chi nhánh
                </Button>
              )}
            </CardHeader>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Tên chi nhánh</TableHead>
                    <TableHead>Địa chỉ</TableHead>
                    <TableHead>Điện thoại</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    {canManageBranch && <TableHead />}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {addingBranch && <NewBranchRow onClose={() => setAddingBranch(false)} />}
                  {branchesQuery.data?.map((b) => (
                    <BranchRow
                      key={b.id}
                      branch={b}
                      canManage={canManageBranch}
                      canDelete={(branchesQuery.data?.length ?? 0) > 1}
                    />
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

const QR_PREVIEW_AMOUNT = 10000;

/** Xem trước QR VietQR "thật" dựng ngay từ dữ liệu đang nhập (chưa cần lưu) — cho phép quét thử
 * bằng app ngân hàng để xác nhận đúng tài khoản trước khi lưu cấu hình. */
function QrPreview({
  bankBin,
  accountNumber,
  accountName,
}: {
  bankBin?: string;
  accountNumber?: string;
  accountName?: string;
}) {
  const payload = useMemo(() => {
    if (!bankBin || !accountNumber) return null;
    return generateVietQrPayload(bankBin, accountNumber, accountName ?? "", QR_PREVIEW_AMOUNT, "Xem thu QR");
  }, [bankBin, accountNumber, accountName]);

  if (!payload) {
    return (
      <p className="text-xs text-muted-foreground">
        Chọn ngân hàng và nhập số tài khoản để xem trước mã QR chuyển khoản thật.
      </p>
    );
  }

  return (
    <div className="flex items-center gap-3 rounded-md border p-3">
      <QRCodeSVG value={payload} size={96} />
      <div className="text-xs text-muted-foreground">
        <div className="font-medium text-foreground">Xem trước QR thật</div>
        <div>
          Quét thử bằng app ngân hàng để xác nhận đúng tài khoản (số tiền demo{" "}
          {QR_PREVIEW_AMOUNT.toLocaleString("vi-VN")}đ). Trên hóa đơn thật, QR sẽ tự điền đúng số
          tiền của từng đơn.
        </div>
      </div>
    </div>
  );
}

function NewBranchRow({ onClose }: { onClose: () => void }) {
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [phone, setPhone] = useState("");
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => createBranch({ name, address, phone }),
    onSuccess: () => {
      toast({ title: "Đã thêm chi nhánh" });
      queryClient.invalidateQueries({ queryKey: ["branches"] });
      onClose();
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể thêm", description: getApiErrorMessage(err) });
    },
  });

  return (
    <TableRow>
      <TableCell>
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Tên chi nhánh"
          className="h-8"
        />
      </TableCell>
      <TableCell>
        <Input
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          placeholder="Địa chỉ"
          className="h-8"
        />
      </TableCell>
      <TableCell>
        <Input
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="Điện thoại"
          className="h-8"
        />
      </TableCell>
      <TableCell />
      <TableCell>
        <div className="flex gap-1">
          <Button
            size="sm"
            disabled={!name.trim() || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            Lưu
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}

function BranchRow({
  branch,
  canManage,
  canDelete,
}: {
  branch: Branch;
  canManage: boolean;
  canDelete: boolean;
}) {
  const [editing, setEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [name, setName] = useState(branch.name);
  const [address, setAddress] = useState(branch.address ?? "");
  const [phone, setPhone] = useState(branch.phone ?? "");
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => updateBranch(branch.id, { name, address, phone }),
    onSuccess: () => {
      toast({ title: "Đã lưu chi nhánh" });
      queryClient.invalidateQueries({ queryKey: ["branches"] });
      setEditing(false);
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể lưu", description: getApiErrorMessage(err) });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteBranch(branch.id),
    onSuccess: () => {
      toast({ title: "Đã xóa chi nhánh" });
      queryClient.invalidateQueries({ queryKey: ["branches"] });
      setConfirmingDelete(false);
    },
    onError: (err) => {
      toast({ variant: "destructive", title: "Không thể xóa", description: getApiErrorMessage(err) });
      setConfirmingDelete(false);
    },
  });

  if (!editing) {
    return (
      <TableRow>
        <TableCell className="font-medium">{branch.name}</TableCell>
        <TableCell>{branch.address ?? "—"}</TableCell>
        <TableCell>{branch.phone ?? "—"}</TableCell>
        <TableCell>
          {branch.active ? (
            <Badge variant="success">Đang hoạt động</Badge>
          ) : (
            <Badge variant="secondary">Ngừng hoạt động</Badge>
          )}
        </TableCell>
        {canManage && (
          <TableCell>
            <div className="flex gap-1">
              <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setEditing(true)}>
                <Pencil className="h-4 w-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-destructive hover:text-destructive"
                disabled={!canDelete}
                title={canDelete ? undefined : "Phải giữ lại ít nhất 1 chi nhánh"}
                onClick={() => setConfirmingDelete(true)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
            <ConfirmDialog
              open={confirmingDelete}
              onOpenChange={setConfirmingDelete}
              title={`Xóa chi nhánh "${branch.name}"?`}
              description="Hành động này không thể hoàn tác."
              variant="destructive"
              loading={deleteMutation.isPending}
              onConfirm={() => deleteMutation.mutate()}
            />
          </TableCell>
        )}
      </TableRow>
    );
  }

  return (
    <TableRow>
      <TableCell>
        <Input value={name} onChange={(e) => setName(e.target.value)} className="h-8" />
      </TableCell>
      <TableCell>
        <Input value={address} onChange={(e) => setAddress(e.target.value)} className="h-8" />
      </TableCell>
      <TableCell>
        <Input value={phone} onChange={(e) => setPhone(e.target.value)} className="h-8" />
      </TableCell>
      <TableCell>
        {branch.active ? (
          <Badge variant="success">Đang hoạt động</Badge>
        ) : (
          <Badge variant="secondary">Ngừng hoạt động</Badge>
        )}
      </TableCell>
      <TableCell>
        <div className="flex gap-1">
          <Button
            size="sm"
            disabled={!name.trim() || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            Lưu
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setEditing(false)}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}
