import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { NumberInput } from "@/components/common/NumberInput";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import type { SettingsTabProps } from "@/pages/settings/settingsTabProps";

/** Tach rieng khoi SettingsPage.tsx (phat hien qua audit production readiness 2026-07-17) — form
 * state/saveMutation VAN o component cha (SettingsPage), truyen xuong qua props, KHONG tach state
 * rieng cho tung tab de tranh doi lai thiet ke useSettingsForm() da co chu y ky (chi ap du lieu
 * server 1 lan dau, tranh ghi de chinh sua chua luu o tab khac — xem comment trong hook do). */
export function SalesSettingsTab({ form, set, canEdit, loading, saving, onSave }: SettingsTabProps) {
  return (
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
          <NumberInput
            id="debt-limit-default"
            disabled={!canEdit || loading}
            min={0}
            value={form.debt_limit_default ?? "0"}
            onValueChange={(v) => set("debt_limit_default", String(v ?? 0))}
          />
        </div>
        {canEdit && (
          <Button
            disabled={loading || saving}
            onClick={() =>
              onSave([
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
  );
}
