import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { Money } from "@/components/common/Money";
import { StatusBadge } from "@/components/common/StatusBadge";
import { cn } from "@/lib/utils";
import { getDashboardSummary, type LowStockItem, type RecentOrder } from "@/lib/api/dashboard";
import type { TopProduct } from "@/lib/api/reports";

const CHART_COLOR = "hsl(168 83% 26%)";
const numberFormatter = new Intl.NumberFormat("vi-VN");

function formatToday(): string {
  const label = new Date().toLocaleDateString("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("-");
  return `${day}/${month}/${year}`;
}

function KpiDelta({ value, suffix }: { value: number; suffix: string }) {
  const positive = value >= 0;
  return (
    <span className={cn("text-xs font-medium", positive ? "text-success" : "text-destructive")}>
      {positive ? "▲" : "▼"} {Math.abs(value)}% {suffix}
    </span>
  );
}

/** Tong quan (FH-3) — so lieu that qua /dashboard/summary (DashboardService phia Backend), tai
 * su dung cong thuc doanh thu/lai gop/top san pham voi module Bao cao (Phase 10). Bang "Ton thap"
 * can branchId cu the (chua co endpoint danh sach chi nhanh — xem ghi chu Phase 10) nen rong cho
 * den khi chon duoc chi nhanh trong Cai dat/Topbar. */
export function DashboardPage() {
  const summaryQuery = useQuery({
    queryKey: ["dashboard", "summary"],
    queryFn: () => getDashboardSummary(),
  });
  const summary = summaryQuery.data;

  const topProductColumns: DataTableColumn<TopProduct>[] = [
    { key: "productName", header: "Sản phẩm" },
    { key: "quantitySold", header: "SL", className: "text-right" },
    {
      key: "revenue",
      header: "Doanh thu",
      className: "text-right",
      render: (row) => <Money value={row.revenue} />,
    },
  ];

  const lowStockColumns: DataTableColumn<LowStockItem>[] = [
    { key: "productName", header: "Sản phẩm" },
    { key: "sku", header: "SKU" },
    { key: "stock", header: "Tồn", className: "text-right" },
    {
      key: "nearestBatchCode",
      header: "Lô / HSD",
      render: (row) =>
        row.nearestBatchCode ? (
          <span>
            {row.nearestBatchCode}
            {row.nearestExpiryDate && ` · HSD ${formatDate(row.nearestExpiryDate)}`}
          </span>
        ) : (
          <span className="text-muted-foreground">—</span>
        ),
    },
  ];

  const recentOrderColumns: DataTableColumn<RecentOrder>[] = [
    { key: "orderNumber", header: "Mã đơn" },
    { key: "customerName", header: "Khách" },
    {
      key: "totalAmount",
      header: "Tổng tiền",
      className: "text-right",
      render: (row) => <Money value={row.totalAmount} />,
    },
    { key: "status", header: "Trạng thái", render: (row) => <StatusBadge status={row.status} /> },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Tổng quan hôm nay</h1>
          <p className="text-sm text-muted-foreground">{formatToday()}</p>
        </div>
        <Button asChild size="lg">
          <Link to="/pos">
            <Plus className="h-4 w-4" />
            Bán hàng (F1)
          </Link>
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Doanh thu</CardDescription>
            <CardTitle className="text-2xl">
              <Money value={summary?.todayRevenue ?? 0} />
            </CardTitle>
          </CardHeader>
          <CardContent>
            {summary?.revenueChangePercent != null && (
              <KpiDelta value={summary.revenueChangePercent} suffix="so với hôm qua" />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Đơn hàng</CardDescription>
            <CardTitle className="text-2xl">{summary?.todayOrderCount ?? 0} đơn</CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            {summary && (
              <>
                {summary.orderCountDelta >= 0 ? "▲" : "▼"} {Math.abs(summary.orderCountDelta)} đơn ·
                TB <Money value={summary.averageOrderValue} />/đơn
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Lãi gộp (ước tính)</CardDescription>
            <CardTitle className="text-2xl">
              <Money value={summary?.grossProfitToday ?? 0} />
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            Biên {summary?.grossProfitMarginPercent ?? 0}%
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Khách trả hàng</CardDescription>
            <CardTitle className="text-2xl">{summary?.returnCountToday ?? 0} đơn</CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-destructive">
            {summary && summary.refundAmountToday > 0 && (
              <>
                −<Money value={summary.refundAmountToday} /> hoàn trả
              </>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Doanh thu 7 ngày gần nhất</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={summary?.last7Days ?? []}
                  margin={{ top: 8, right: 8, left: 8, bottom: 8 }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="hsl(var(--border))" />
                  <XAxis
                    dataKey="label"
                    tick={{ fontSize: 12 }}
                    tickLine={false}
                    axisLine={{ stroke: "hsl(var(--border))" }}
                  />
                  <YAxis
                    tick={{ fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(v) => numberFormatter.format(v)}
                    width={70}
                  />
                  <Tooltip
                    formatter={(value: number) => [`${numberFormatter.format(value)} đ`, "Doanh thu"]}
                    contentStyle={{ borderRadius: 8, borderColor: "hsl(var(--border))" }}
                  />
                  <Bar dataKey="revenue" fill={CHART_COLOR} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Top sản phẩm bán chạy</CardTitle>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={topProductColumns}
              data={summary?.topProducts ?? []}
              rowKey={(row) => row.productId}
              loading={summaryQuery.isLoading}
            />
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Tồn thấp</CardTitle>
            <CardDescription>
              Cần chọn chi nhánh cụ thể để xem — chưa có bộ chọn chi nhánh (xem PROJECT_STATE)
            </CardDescription>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={lowStockColumns}
              data={summary?.lowStock ?? []}
              rowKey={(row) => row.id}
              loading={summaryQuery.isLoading}
              emptyMessage="Không có sản phẩm tồn thấp"
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Đơn hàng gần đây</CardTitle>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={recentOrderColumns}
              data={summary?.recentOrders ?? []}
              rowKey={(row) => row.orderNumber}
              loading={summaryQuery.isLoading}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
