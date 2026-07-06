import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useAppSelector } from "@/store/hooks";

export function DashboardPage() {
  const user = useAppSelector((state) => state.auth.user);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Xin chào, {user?.fullName}</h1>
        <p className="text-muted-foreground">Tổng quan cửa hàng hôm nay</p>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { label: "Doanh thu hôm nay", value: "—" },
          { label: "Đơn hàng hôm nay", value: "—" },
          { label: "Sản phẩm sắp hết hàng", value: "—" },
          { label: "Công nợ quá hạn", value: "—" },
        ].map((kpi) => (
          <Card key={kpi.label}>
            <CardHeader className="pb-2">
              <CardDescription>{kpi.label}</CardDescription>
              <CardTitle className="text-3xl">{kpi.value}</CardTitle>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground">
              Số liệu báo cáo đầy đủ ở Phase 10
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
