import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Công nợ", () => {
  test("hiển thị KPI và bảng đổi chiều công nợ khách hàng/nhà cung cấp", async ({ page }) => {
    await login(page);
    await page.goto("/debts");

    // "Phải thu khách hàng" xuat hien ca o the KPI (CardDescription) lan tieu de bang doi tac
    // (CardTitle theo direction mac dinh) — .first() chi de xac nhan text co mat, khong phan
    // biet dung node nao.
    await expect(page.getByRole("heading", { name: "Công nợ" })).toBeVisible();
    await expect(page.getByText("Phải thu khách hàng").first()).toBeVisible();
    await expect(page.getByText("Phải trả nhà cung cấp").first()).toBeVisible();

    await page.getByRole("tab", { name: /phải trả ncc/i }).click();
    await expect(page.getByRole("columnheader", { name: "Nhà cung cấp" })).toBeVisible();
  });
});
