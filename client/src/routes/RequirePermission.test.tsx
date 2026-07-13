import { configureStore } from "@reduxjs/toolkit";
import { render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { describe, expect, it } from "vitest";
import { RequirePermission } from "@/routes/RequirePermission";
import authReducer from "@/store/slices/authSlice";
import cartReducer from "@/store/slices/cartSlice";
import uiReducer from "@/store/slices/uiSlice";

function renderWithPermissions(permissions: string[], ui: React.ReactElement) {
  const store = configureStore({
    reducer: { auth: authReducer, cart: cartReducer, ui: uiReducer },
    preloadedState: { auth: { user: null, accessToken: null, permissions } },
  });
  return render(<Provider store={store}>{ui}</Provider>);
}

describe("RequirePermission", () => {
  it("render duoc trang khi user co quyen", () => {
    renderWithPermissions(
      ["product:view"],
      <RequirePermission perm="product:view">
        <div>Trang san pham</div>
      </RequirePermission>,
    );
    expect(screen.getByText("Trang san pham")).toBeInTheDocument();
  });

  it("chan ca trang va hien ForbiddenPage (403) khi thieu quyen - khac PermissionGate o cho nay", () => {
    renderWithPermissions(
      ["order:view"],
      <RequirePermission perm="product:view">
        <div>Trang san pham</div>
      </RequirePermission>,
    );
    expect(screen.queryByText("Trang san pham")).not.toBeInTheDocument();
    expect(screen.getByText("Không có quyền truy cập")).toBeInTheDocument();
  });

  it("requireAll=true chan khi thieu 1 trong nhieu quyen yeu cau", () => {
    renderWithPermissions(
      ["order:view"],
      <RequirePermission perm={["product:view", "order:view"]} requireAll>
        <div>Can du 2 quyen</div>
      </RequirePermission>,
    );
    expect(screen.getByText("Không có quyền truy cập")).toBeInTheDocument();
  });

  it("mang quyen (OR mac dinh) - cho qua khi co it nhat 1 quyen trong danh sach", () => {
    renderWithPermissions(
      ["order:view"],
      <RequirePermission perm={["product:view", "order:view"]}>
        <div>Duoc phep</div>
      </RequirePermission>,
    );
    expect(screen.getByText("Duoc phep")).toBeInTheDocument();
  });
});
