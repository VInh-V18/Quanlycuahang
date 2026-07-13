import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { configureStore } from "@reduxjs/toolkit";
import { Provider } from "react-redux";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CustomerFormDialog, SupplierFormDialog } from "@/pages/partners/PartnersPage";
import { PermissionGate } from "@/components/common/PermissionGate";
import authReducer from "@/store/slices/authSlice";
import cartReducer from "@/store/slices/cartSlice";
import uiReducer from "@/store/slices/uiSlice";
import * as customersApi from "@/lib/api/customers";
import * as suppliersApi from "@/lib/api/suppliers";
import type { CustomerListItem } from "@/lib/api/customers";
import type { Supplier } from "@/lib/api/suppliers";

vi.mock("@/lib/api/customers", async () => {
  const actual = await vi.importActual<typeof customersApi>("@/lib/api/customers");
  return { ...actual, createCustomer: vi.fn(), updateCustomer: vi.fn() };
});
vi.mock("@/lib/api/suppliers", async () => {
  const actual = await vi.importActual<typeof suppliersApi>("@/lib/api/suppliers");
  return { ...actual, createSupplier: vi.fn(), updateSupplier: vi.fn() };
});

function renderWithQueryClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const sampleCustomer: CustomerListItem = {
  id: 1,
  name: "Nguyễn Văn A",
  phone: "0901234567",
  address: "123 Lê Lợi",
  customerGroupId: null,
  groupName: null,
  debtLimit: 500_000,
  totalPurchased: 0,
  orderCount: 0,
  lastPurchaseAt: null,
  currentDebt: 0,
};

const sampleSupplier: Supplier = {
  id: 2,
  name: "NCC Rau củ Đà Lạt",
  phone: "0987654321",
  address: "Đà Lạt",
  outstandingDebt: 0,
  totalPurchased: 0,
  orderCount: 0,
  lastPurchaseAt: null,
};

describe("CustomerFormDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị 'Thêm khách hàng' và nút Lưu bị vô hiệu khi tên rỗng (submit lỗi validation)", () => {
    renderWithQueryClient(
      <CustomerFormDialog groups={[]} listQueryKey={["customers"]} onClose={() => {}} />,
    );
    expect(screen.getByText("Thêm khách hàng")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Lưu" })).toBeDisabled();
  });

  it("chế độ sửa hiển thị 'Sửa khách hàng' và điền sẵn dữ liệu hiện có", () => {
    renderWithQueryClient(
      <CustomerFormDialog
        groups={[]}
        customer={sampleCustomer}
        listQueryKey={["customers"]}
        onClose={() => {}}
      />,
    );
    expect(screen.getByText("Sửa khách hàng")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Nguyễn Văn A")).toBeInTheDocument();
    expect(screen.getByDisplayValue("0901234567")).toBeInTheDocument();
  });

  it("submit hợp lệ gọi createCustomer đúng dữ liệu và đóng dialog", async () => {
    const onClose = vi.fn();
    vi.mocked(customersApi.createCustomer).mockResolvedValue({
      id: 9,
      name: "Khách mới",
      phone: null,
      address: null,
      email: null,
      customerGroupId: null,
      debtLimit: 0,
    });

    renderWithQueryClient(
      <CustomerFormDialog groups={[]} listQueryKey={["customers"]} onClose={onClose} />,
    );
    fireEvent.change(screen.getByLabelText("Tên khách hàng"), {
      target: { value: "Khách mới" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(customersApi.createCustomer).toHaveBeenCalledWith(
      expect.objectContaining({ name: "Khách mới" }),
    );
  });

  it("submit thất bại (lỗi API) KHÔNG đóng dialog để người dùng sửa lại", async () => {
    const onClose = vi.fn();
    vi.mocked(customersApi.updateCustomer).mockRejectedValue(new Error("network"));

    renderWithQueryClient(
      <CustomerFormDialog
        groups={[]}
        customer={sampleCustomer}
        listQueryKey={["customers"]}
        onClose={onClose}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    await waitFor(() => expect(customersApi.updateCustomer).toHaveBeenCalled());
    expect(onClose).not.toHaveBeenCalled();
  });
});

describe("SupplierFormDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("nút Lưu bị vô hiệu khi tên rỗng (submit lỗi validation)", () => {
    renderWithQueryClient(<SupplierFormDialog listQueryKey={["suppliers"]} onClose={() => {}} />);
    expect(screen.getByRole("button", { name: "Lưu" })).toBeDisabled();
  });

  it("chế độ sửa hiển thị 'Sửa nhà cung cấp' và điền sẵn dữ liệu hiện có", () => {
    renderWithQueryClient(
      <SupplierFormDialog
        supplier={sampleSupplier}
        listQueryKey={["suppliers"]}
        onClose={() => {}}
      />,
    );
    expect(screen.getByText("Sửa nhà cung cấp")).toBeInTheDocument();
    expect(screen.getByDisplayValue("NCC Rau củ Đà Lạt")).toBeInTheDocument();
  });

  it("submit hợp lệ gọi updateSupplier đúng id và đóng dialog", async () => {
    const onClose = vi.fn();
    vi.mocked(suppliersApi.updateSupplier).mockResolvedValue(sampleSupplier);

    renderWithQueryClient(
      <SupplierFormDialog
        supplier={sampleSupplier}
        listQueryKey={["suppliers"]}
        onClose={onClose}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(suppliersApi.updateSupplier).toHaveBeenCalledWith(
      2,
      expect.objectContaining({ name: sampleSupplier.name }),
    );
  });
});

/** "Quyền bị ẩn": mô phỏng đúng cách cột hành động của bảng KH/NCC bọc nút "Sửa" bằng
 * PermissionGate(customer:update) trong PartnersPage thật — xác nhận ẩn/hiện đúng theo quyền. */
describe("PartnersPage - ẩn hành động Sửa theo quyền", () => {
  function renderActionCell(permissions: string[]) {
    const store = configureStore({
      reducer: { auth: authReducer, cart: cartReducer, ui: uiReducer },
      preloadedState: { auth: { user: null, accessToken: null, permissions } },
    });
    return render(
      <Provider store={store}>
        <PermissionGate perm="customer:update">
          <button>Sửa</button>
        </PermissionGate>
      </Provider>,
    );
  }

  it("ẩn nút Sửa khi thiếu quyền customer:update", () => {
    renderActionCell(["customer:view"]);
    expect(screen.queryByText("Sửa")).not.toBeInTheDocument();
  });

  it("hiện nút Sửa khi có quyền customer:update", () => {
    renderActionCell(["customer:view", "customer:update"]);
    expect(screen.getByText("Sửa")).toBeInTheDocument();
  });
});
