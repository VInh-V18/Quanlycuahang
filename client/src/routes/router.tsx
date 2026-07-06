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
    ],
  },
  { path: "*", element: <NotFoundPage /> },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
