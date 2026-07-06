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
