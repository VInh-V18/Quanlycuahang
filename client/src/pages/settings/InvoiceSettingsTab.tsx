import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { SettingsTabProps } from "@/pages/settings/settingsTabProps";

/** Tach rieng khoi SettingsPage.tsx (phat hien qua audit production readiness 2026-07-17) — xem
 * ghi chu tuong tu o SalesSettingsTab.tsx ve ly do khong tach rieng state. */
export function InvoiceSettingsTab({ form, set, canEdit, loading, saving, onSave }: SettingsTabProps) {
  return (
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
            disabled={loading || saving}
            onClick={() => onSave(["order_number_prefix", "invoice_number_prefix", "sku_prefix"])}
          >
            Lưu thay đổi
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
