import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Đăng nhập", () => {
  test("đăng nhập đúng thông tin điều hướng vào Tổng quan", async ({ page }) => {
    await login(page);
    await expect(page.getByRole("heading", { name: "Tổng quan hôm nay" })).toBeVisible();
  });

  test("sai mật khẩu hiển thị lỗi và ở lại trang đăng nhập", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel(/tên đăng nhập|username/i).fill("owner01");
    await page.getByLabel(/mật khẩu|password/i).fill("mat-khau-sai-12345");
    await page.getByRole("button", { name: /đăng nhập/i }).click();

    // Toast Radix render 2 node cung text (div hien thi + span aria-live cho screen reader).
    await expect(page.getByText("Đăng nhập thất bại").first()).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });
});
