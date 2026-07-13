import { configureStore } from "@reduxjs/toolkit";
import { render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { describe, expect, it } from "vitest";
import { PermissionGate } from "@/components/common/PermissionGate";
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

describe("PermissionGate", () => {
  it("hien children khi user co dung quyen yeu cau (1 quyen, string)", () => {
    renderWithPermissions(
      ["product:view"],
      <PermissionGate perm="product:view">
        <div>Noi dung san pham</div>
      </PermissionGate>,
    );
    expect(screen.getByText("Noi dung san pham")).toBeInTheDocument();
  });

  it("an children va hien fallback khi user KHONG co quyen", () => {
    renderWithPermissions(
      ["order:view"],
      <PermissionGate perm="product:view" fallback={<div>Khong co quyen</div>}>
        <div>Noi dung san pham</div>
      </PermissionGate>,
    );
    expect(screen.queryByText("Noi dung san pham")).not.toBeInTheDocument();
    expect(screen.getByText("Khong co quyen")).toBeInTheDocument();
  });

  it("mang quyen (OR mac dinh) - hien khi co IT NHAT 1 trong danh sach", () => {
    renderWithPermissions(
      ["order:view"],
      <PermissionGate perm={["product:view", "order:view"]}>
        <div>Duoc phep (OR)</div>
      </PermissionGate>,
    );
    expect(screen.getByText("Duoc phep (OR)")).toBeInTheDocument();
  });

  it("requireAll=true - chan khi thieu 1 trong nhieu quyen yeu cau (AND)", () => {
    renderWithPermissions(
      ["order:view"],
      <PermissionGate perm={["product:view", "order:view"]} requireAll>
        <div>Can du ca 2 quyen</div>
      </PermissionGate>,
    );
    expect(screen.queryByText("Can du ca 2 quyen")).not.toBeInTheDocument();
  });

  it("requireAll=true - hien khi co du tat ca quyen yeu cau", () => {
    renderWithPermissions(
      ["order:view", "product:view"],
      <PermissionGate perm={["product:view", "order:view"]} requireAll>
        <div>Can du ca 2 quyen</div>
      </PermissionGate>,
    );
    expect(screen.getByText("Can du ca 2 quyen")).toBeInTheDocument();
  });

  it("khong co fallback -> khong render gi (khong loi) khi bi chan", () => {
    const { container } = renderWithPermissions(
      [],
      <PermissionGate perm="product:view">
        <div>An di</div>
      </PermissionGate>,
    );
    expect(container).toBeEmptyDOMElement();
  });
});
