import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { DataTable, type DataTableColumn } from "@/components/common/DataTable";
import { DateRangePicker } from "@/components/common/DateRangePicker";
import { Money } from "@/components/common/Money";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  downloadReportExcel,
  getDebtAging,
  getEmployeePerformance,
  getGrossProfit,
  getInventoryValue,
  getRevenue,
  getTopCustomers,
  getTopProducts,
  type DebtAgingBucket,
  type EmployeePerformance,
  type InventoryValue,
  type RevenueGroupBy,
  type TopCustomer,
  type TopProduct,
} from "@/lib/api/reports";

const CHART_COLOR = "hsl(217, 91%, 60%)";
const numberFormatter = new Intl.NumberFormat("vi-VN");

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/** Bao cao (Phase 10) — doanh thu/loi nhuan gop/top SP/top KH/hieu suat NV/ton kho/cong no.
 * Chua co endpoint danh sach chi nhanh (xem PROJECT_STATE) nen bo loc chi nhanh tam thoi bo qua,
 * moi bao cao mac dinh tinh tren toan bo (branchId = undefined). */
export function ReportsPage() {
  const [range, setRange] = useState<{ from: Date; to: Date }>(() => {
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 30);
    return { from, to };
  });
  const [revenueGroupBy, setRevenueGroupBy] = useState<RevenueGroupBy>("day");
  const [inventoryGroupBy, setInventoryGroupBy] = useState<"branch" | "category">("category");
  const [debtDirection, setDebtDirection] = useState<"receivable" | "payable">("receivable");

  const params = useMemo(
    () => ({ from: toIsoDate(range.from), to: toIsoDate(range.to) }),
    [range],
  );

  const revenueQuery = useQuery({
    queryKey: ["reports", "revenue", params, revenueGroupBy],
    queryFn: () => getRevenue({ ...params, groupBy: revenueGroupBy }),
  });

  const grossProfitQuery = useQuery({
    queryKey: ["reports", "gross-profit", params],
    queryFn: () => getGrossProfit(params),
  });

  const topProductsQuery = useQuery({
    queryKey: ["reports", "top-products", params],
    queryFn: () => getTopProducts({ ...params, limit: 10 }),
  });

  const topCustomersQuery = useQuery({
    queryKey: ["reports", "top-customers", params],
    queryFn: () => getTopCustomers({ ...params, limit: 10 }),
  });

  const employeeQuery = useQuery({
    queryKey: ["reports", "employee-performance", params],
    queryFn: () => getEmployeePerformance(params),
  });

  const inventoryQuery = useQuery({
    queryKey: ["reports", "inventory-value", inventoryGroupBy],
    queryFn: () => getInventoryValue({ groupBy: inventoryGroupBy }),
  });

  const debtQuery = useQuery({
    queryKey: ["reports", "debt-aging", debtDirection],
    queryFn: () => getDebtAging(debtDirection),
  });

  const topProductColumns: DataTableColumn<TopProduct>[] = [
    { key: "sku", header: "SKU" },
    { key: "productName", header: "Sản phẩm" },
    { key: "quantitySold", header: "SL bán", className: "text-right" },
    {
      key: "revenue",
      header: "Doanh thu",
      className: "text-right",
      render: (row) => <Money value={row.revenue} />,
    },
  ];

  const topCustomerColumns: DataTableColumn<TopCustomer>[] = [
    { key: "customerName", header: "Khách hàng" },
    { key: "orderCount", header: "Số đơn", className: "text-right" },
    {
      key: "revenue",
      header: "Doanh thu",
      className: "text-right",
      render: (row) => <Money value={row.revenue} />,
    },
  ];

  const employeeColumns: DataTableColumn<EmployeePerformance>[] = [
    { key: "cashierName", header: "Thu ngân" },
    { key: "orderCount", header: "Số đơn", className: "text-right" },
    {
      key: "revenue",
      header: "Doanh thu",
      className: "text-right",
      render: (row) => <Money value={row.revenue} />,
    },
    {
      key: "averageOrderValue",
      header: "TB/đơn",
      className: "text-right",
      render: (row) => <Money value={row.averageOrderValue} />,
    },
  ];

  const inventoryColumns: DataTableColumn<InventoryValue>[] = [
    { key: "label", header: inventoryGroupBy === "branch" ? "Chi nhánh" : "Danh mục" },
    { key: "totalQuantity", header: "Số lượng", className: "text-right" },
    {
      key: "totalValue",
      header: "Giá trị tồn kho",
      className: "text-right",
      render: (row) => <Money value={row.totalValue} />,
    },
  ];

  const debtColumns: DataTableColumn<DebtAgingBucket>[] = [
    { key: "bucket", header: "Tuổi nợ (ngày)" },
    { key: "debtCount", header: "Số khoản", className: "text-right" },
    {
      key: "totalAmount",
      header: "Tổng tiền",
      className: "text-right",
      render: (row) => <Money value={row.totalAmount} />,
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold">Báo cáo</h1>
        <DateRangePicker
          value={range}
          onChange={(r) => r?.from && r?.to && setRange({ from: r.from, to: r.to })}
        />
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <div>
            <CardTitle>Doanh thu</CardTitle>
            <CardDescription>Theo {revenueGroupBy === "day" ? "ngày" : revenueGroupBy === "week" ? "tuần" : revenueGroupBy === "month" ? "tháng" : revenueGroupBy === "branch" ? "chi nhánh" : "thu ngân"}</CardDescription>
          </div>
          <div className="flex gap-2">
            <Select value={revenueGroupBy} onValueChange={(v) => setRevenueGroupBy(v as RevenueGroupBy)}>
              <SelectTrigger className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="day">Theo ngày</SelectItem>
                <SelectItem value="week">Theo tuần</SelectItem>
                <SelectItem value="month">Theo tháng</SelectItem>
                <SelectItem value="branch">Theo chi nhánh</SelectItem>
                <SelectItem value="cashier">Theo thu ngân</SelectItem>
              </SelectContent>
            </Select>
            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                downloadReportExcel(
                  "revenue",
                  { ...params, groupBy: revenueGroupBy },
                  "doanh-thu.xlsx",
                )
              }
            >
              <Download className="mr-2 h-4 w-4" />
              Xuất Excel
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={revenueQuery.data ?? []} margin={{ top: 8, right: 8, left: 8, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="hsl(214 32% 91%)" />
                <XAxis dataKey="label" tick={{ fontSize: 12 }} tickLine={false} axisLine={{ stroke: "hsl(214 32% 91%)" }} />
                <YAxis
                  tick={{ fontSize: 12 }}
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(v) => numberFormatter.format(v)}
                  width={80}
                />
                <Tooltip
                  formatter={(value: number) => [`${numberFormatter.format(value)} đ`, "Doanh thu"]}
                  contentStyle={{ borderRadius: 8, borderColor: "hsl(214 32% 91%)" }}
                />
                <Bar dataKey="revenue" fill={CHART_COLOR} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Lợi nhuận gộp</CardTitle>
          <CardDescription>Doanh thu − Giá vốn hàng bán − Ảnh hưởng hoàn trả</CardDescription>
        </CardHeader>
        <CardContent>
          {grossProfitQuery.data && (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div>
                <div className="text-sm text-muted-foreground">Doanh thu</div>
                <div className="text-xl font-semibold"><Money value={grossProfitQuery.data.revenue} /></div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Giá vốn hàng bán</div>
                <div className="text-xl font-semibold"><Money value={grossProfitQuery.data.costOfGoodsSold} /></div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Ảnh hưởng hoàn trả</div>
                <div className="text-xl font-semibold text-destructive">
                  <Money value={grossProfitQuery.data.returnImpact} />
                </div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Lợi nhuận gộp</div>
                <div className="text-xl font-semibold text-success"><Money value={grossProfitQuery.data.grossProfit} /></div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <CardTitle>Top sản phẩm</CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={() => downloadReportExcel("top-products", { ...params, limit: 10 }, "top-san-pham.xlsx")}
            >
              <Download className="h-4 w-4" />
            </Button>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={topProductColumns}
              data={topProductsQuery.data ?? []}
              rowKey={(row) => row.productId}
              loading={topProductsQuery.isLoading}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Top khách hàng</CardTitle>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={topCustomerColumns}
              data={topCustomersQuery.data ?? []}
              rowKey={(row) => row.customerId}
              loading={topCustomersQuery.isLoading}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <CardTitle>Hiệu suất nhân viên</CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={() => downloadReportExcel("employee-performance", params, "hieu-suat-nhan-vien.xlsx")}
            >
              <Download className="h-4 w-4" />
            </Button>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={employeeColumns}
              data={employeeQuery.data ?? []}
              rowKey={(row) => row.cashierName}
              loading={employeeQuery.isLoading}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <CardTitle>Giá trị tồn kho</CardTitle>
            <div className="flex gap-2">
              <Tabs value={inventoryGroupBy} onValueChange={(v) => setInventoryGroupBy(v as "branch" | "category")}>
                <TabsList>
                  <TabsTrigger value="category">Theo danh mục</TabsTrigger>
                  <TabsTrigger value="branch">Theo chi nhánh</TabsTrigger>
                </TabsList>
              </Tabs>
              <Button
                variant="outline"
                size="sm"
                onClick={() =>
                  downloadReportExcel("inventory-value", { groupBy: inventoryGroupBy }, "gia-tri-ton-kho.xlsx")
                }
              >
                <Download className="h-4 w-4" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={inventoryColumns}
              data={inventoryQuery.data ?? []}
              rowKey={(row) => row.label}
              loading={inventoryQuery.isLoading}
            />
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <CardTitle>Công nợ theo tuổi nợ</CardTitle>
            <Tabs value={debtDirection} onValueChange={(v) => setDebtDirection(v as "receivable" | "payable")}>
              <TabsList>
                <TabsTrigger value="receivable">Khách hàng nợ</TabsTrigger>
                <TabsTrigger value="payable">Phải trả NCC</TabsTrigger>
              </TabsList>
            </Tabs>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={debtColumns}
              data={debtQuery.data ?? []}
              rowKey={(row) => row.bucket}
              loading={debtQuery.isLoading}
              emptyMessage="Không có công nợ"
            />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
