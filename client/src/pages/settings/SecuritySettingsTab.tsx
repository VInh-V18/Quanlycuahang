import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { NumberInput } from "@/components/common/NumberInput";
import type { SettingsTabProps } from "@/pages/settings/settingsTabProps";

/** Tach rieng khoi SettingsPage.tsx (phat hien qua audit production readiness 2026-07-17) — xem
 * ghi chu tuong tu o SalesSettingsTab.tsx ve ly do khong tach rieng state. */
export function SecuritySettingsTab({
  form,
  set,
  canEdit,
  loading,
  saving,
  onSave,
  systemInfo,
}: SettingsTabProps & { systemInfo: Record<string, string> | undefined }) {
  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Bảo mật đăng nhập</CardTitle>
          <CardDescription>Chống dò mật khẩu bằng giới hạn số lần đăng nhập sai</CardDescription>
        </CardHeader>
        <CardContent className="max-w-lg space-y-4">
          <div>
            <Label htmlFor="login-rate-limit">Số lần đăng nhập sai tối đa (trong 15 phút)</Label>
            <NumberInput
              id="login-rate-limit"
              allowDecimal={false}
              min={1}
              disabled={!canEdit || loading}
              value={form.login_rate_limit_attempts ?? "5"}
              onValueChange={(v) => set("login_rate_limit_attempts", String(v ?? 1))}
            />
            <p className="mt-1 text-xs text-muted-foreground">
              Vượt quá số lần này, tài khoản bị khóa đăng nhập tạm thời trong 15 phút
            </p>
          </div>
          {canEdit && (
            <Button disabled={loading || saving} onClick={() => onSave(["login_rate_limit_attempts"])}>
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
              {systemInfo?.accessTokenTtlMinutes ?? "—"} phút
            </div>
          </div>
          <div>
            <div className="text-xs text-muted-foreground">Refresh token hết hạn sau</div>
            <div className="text-lg font-semibold">{systemInfo?.refreshTokenTtlDays ?? "—"} ngày</div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
