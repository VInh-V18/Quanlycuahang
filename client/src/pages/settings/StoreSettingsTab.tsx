import { useMemo, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2, X } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useToast } from "@/components/ui/use-toast";
import { createBranch, deleteBranch, updateBranch, type Branch } from "@/lib/api/settings";
import { getApiErrorMessage } from "@/lib/http/errors";
import { generateVietQrPayload, VIETQR_BANKS } from "@/lib/vietqr";
import type { SettingsTabProps } from "@/pages/settings/settingsTabProps";

interface StoreSettingsTabProps extends SettingsTabProps {
  canManageBranch: boolean;
  branches: Branch[] | undefined;
  uploadingQr: boolean;
  onQrUpload: (file: File) => void;
}

/** Tach rieng khoi SettingsPage.tsx (phat hien qua audit production readiness 2026-07-17) — gom
 * ca QrPreview/NewBranchRow/BranchRow (da la component rieng tu truoc, chi doi noi o) vi ca 3 chi
 * dung trong pham vi tab nay. Xem ghi chu tuong tu o SalesSettingsTab.tsx ve ly do khong tach
 * rieng state form chinh. */
export function StoreSettingsTab({
  form,
  set,
  canEdit,
  loading,
  saving,
  onSave,
  canManageBranch,
  branches,
  uploadingQr,
  onQrUpload,
}: StoreSettingsTabProps) {
  const [addingBranch, setAddingBranch] = useState(false);

  return (
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
                placeholder="VD: 0123456789 - 0987654321"
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
                placeholder="VD: NGUYỄN VĂN A"
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
                      onChange={(e) => e.target.files?.[0] && onQrUpload(e.target.files[0])}
                    />
                  </label>
                )}
              </div>
            </div>
          </div>

          <QrPreview bankBin={form.bank_bin} accountNumber={form.bank_account_number} accountName={form.bank_account_name} />

          {canEdit && (
            <Button
              disabled={loading || saving}
              onClick={() =>
                onSave([
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
              {branches?.map((b) => (
                <BranchRow
                  key={b.id}
                  branch={b}
                  canManage={canManageBranch}
                  canDelete={(branches?.length ?? 0) > 1}
                />
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
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
