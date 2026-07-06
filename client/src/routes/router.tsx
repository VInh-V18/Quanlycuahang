import { lazy, Suspense } from "react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AuthLayout } from "@/components/layout/AuthLayout";
import { MainLayout } from "@/components/layout/MainLayout";
import { PosLayout } from "@/components/layout/PosLayout";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { RequireAuth } from "@/routes/RequireAuth";
import { RequirePermission } from "@/routes/RequirePermission";
import { Skeleton } from "@/components/ui/skeleton";

const LoginPage = lazy(() => import("@/pages/auth/LoginPage").then((m) => ({ default: m.LoginPage })));
const DashboardPage = lazy(() =>
  import("@/pages/DashboardPage").then((m) => ({ default: m.DashboardPage })),
);
const ProductsPage = lazy(() =>
  import("@/pages/products/ProductsPage").then((m) => ({ default: m.ProductsPage })),
);
const ProductFormPage = lazy(() =>
  import("@/pages/products/ProductFormPage").then((m) => ({ default: m.ProductFormPage })),
);
const InventoryPage = lazy(() =>
  import("@/pages/inventory/InventoryPage").then((m) => ({ default: m.InventoryPage })),
);
const PurchaseOrdersPage = lazy(() =>
  import("@/pages/purchase-orders/PurchaseOrdersPage").then((m) => ({
    default: m.PurchaseOrdersPage,
  })),
);
const PurchaseOrderCreatePage = lazy(() =>
  import("@/pages/purchase-orders/PurchaseOrderCreatePage").then((m) => ({
    default: m.PurchaseOrderCreatePage,
  })),
);
const StockTakesPage = lazy(() =>
  import("@/pages/stock-takes/StockTakesPage").then((m) => ({ default: m.StockTakesPage })),
);
const StockTakeNewPage = lazy(() =>
  import("@/pages/stock-takes/StockTakesPage").then((m) => ({ default: m.StockTakeNewPage })),
);
const StockTakeDetailPage = lazy(() =>
  import("@/pages/stock-takes/StockTakeDetailPage").then((m) => ({
    default: m.StockTakeDetailPage,
  })),
);
const OrdersPage = lazy(() =>
  import("@/pages/orders/OrdersPage").then((m) => ({ default: m.OrdersPage })),
);
const ReturnCreatePage = lazy(() =>
  import("@/pages/orders/ReturnCreatePage").then((m) => ({ default: m.ReturnCreatePage })),
);
const ReturnsSearchPage = lazy(() =>
  import("@/pages/orders/ReturnsSearchPage").then((m) => ({ default: m.ReturnsSearchPage })),
);
const PartnersPage = lazy(() =>
  import("@/pages/partners/PartnersPage").then((m) => ({ default: m.PartnersPage })),
);
const DebtsPage = lazy(() =>
  import("@/pages/debts/DebtsPage").then((m) => ({ default: m.DebtsPage })),
);
const EmployeesPage = lazy(() =>
  import("@/pages/employees/EmployeesPage").then((m) => ({ default: m.EmployeesPage })),
);
const ShiftsPage = lazy(() =>
  import("@/pages/shifts/ShiftsPage").then((m) => ({ default: m.ShiftsPage })),
);
const SettingsPage = lazy(() =>
  import("@/pages/settings/SettingsPage").then((m) => ({ default: m.SettingsPage })),
);
const PosPage = lazy(() => import("@/pages/pos/PosPage").then((m) => ({ default: m.PosPage })));
const InvoicePrintPage = lazy(() =>
  import("@/pages/invoices/InvoicePrintPage").then((m) => ({ default: m.InvoicePrintPage })),
);
const InvoiceLookupPage = lazy(() =>
  import("@/pages/invoices/InvoiceLookupPage").then((m) => ({ default: m.InvoiceLookupPage })),
);
const ReportsPage = lazy(() =>
  import("@/pages/reports/ReportsPage").then((m) => ({ default: m.ReportsPage })),
);

function PageFallback() {
  return (
    <div className="space-y-3 p-6">
      <Skeleton className="h-8 w-1/3" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}

function withSuspense(node: React.ReactNode) {
  return <Suspense fallback={<PageFallback />}>{node}</Suspense>;
}

const router = createBrowserRouter([
  {
    element: <AuthLayout />,
    children: [{ path: "/login", element: withSuspense(<LoginPage />) }],
  },
  {
    // Tra cuu hoa don cong khai qua QR - khong bọc RequireAuth (khach khong dang nhap).
    path: "/tra-cuu/:code",
    element: withSuspense(<InvoiceLookupPage />),
  },
  {
    element: (
      <RequireAuth>
        <PosLayout />
      </RequireAuth>
    ),
    children: [
      {
        path: "/pos",
        element: withSuspense(
          <RequirePermission perm="order:create">
            <PosPage />
          </RequirePermission>,
        ),
      },
    ],
  },
  {
    element: (
      <RequireAuth>
        <MainLayout />
      </RequireAuth>
    ),
    children: [
      { path: "/", element: withSuspense(<DashboardPage />) },
      {
        path: "/products",
        element: withSuspense(
          <RequirePermission perm="product:view">
            <ProductsPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/products/new",
        element: withSuspense(
          <RequirePermission perm="product:create">
            <ProductFormPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/products/:id/edit",
        element: withSuspense(
          <RequirePermission perm="product:update">
            <ProductFormPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/orders",
        element: withSuspense(
          <RequirePermission perm="order:view">
            <OrdersPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/orders/:id/return",
        element: withSuspense(
          <RequirePermission perm="return:create">
            <ReturnCreatePage />
          </RequirePermission>,
        ),
      },
      {
        path: "/returns",
        element: withSuspense(
          <RequirePermission perm="return:create">
            <ReturnsSearchPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/inventory",
        element: withSuspense(
          <RequirePermission perm="inventory:view">
            <InventoryPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/purchase-orders",
        element: withSuspense(
          <RequirePermission perm="purchase-order:view">
            <PurchaseOrdersPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/purchase-orders/new",
        element: withSuspense(
          <RequirePermission perm="purchase-order:create">
            <PurchaseOrderCreatePage />
          </RequirePermission>,
        ),
      },
      {
        path: "/stock-takes",
        element: withSuspense(
          <RequirePermission perm="stock-take:view">
            <StockTakesPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/stock-takes/new",
        element: withSuspense(
          <RequirePermission perm="stock-take:create">
            <StockTakeNewPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/stock-takes/:id",
        element: withSuspense(
          <RequirePermission perm="stock-take:view">
            <StockTakeDetailPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/customers",
        element: withSuspense(
          <RequirePermission perm="customer:view">
            <PartnersPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/suppliers",
        element: withSuspense(
          <RequirePermission perm="supplier:view">
            <PartnersPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/debts",
        element: withSuspense(
          <RequirePermission perm="debt:view">
            <DebtsPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/employees",
        element: withSuspense(
          <RequirePermission perm="employee:view">
            <EmployeesPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/shifts",
        element: withSuspense(
          <RequirePermission perm="shift:view">
            <ShiftsPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/settings",
        element: withSuspense(
          <RequirePermission perm="settings:view">
            <SettingsPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/invoices/:id/print",
        element: withSuspense(
          <RequirePermission perm="invoice:view">
            <InvoicePrintPage />
          </RequirePermission>,
        ),
      },
      {
        path: "/reports",
        element: withSuspense(
          <RequirePermission perm="report:revenue">
            <ReportsPage />
          </RequirePermission>,
        ),
      },
    ],
  },
  { path: "*", element: <NotFoundPage /> },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
