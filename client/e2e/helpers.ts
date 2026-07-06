import type { Page } from "@playwright/test";

/** Dang nhap qua form that (khong bypass qua localStorage/token) — dam bao luon test dung luong
 * dang nhap that su nguoi dung se di qua (Phase 11 Gate: E2E chinh thuc). */
export async function login(
  page: Page,
  username = "owner01",
  password = "Password@123",
): Promise<void> {
  await page.goto("/login");
  await page.getByLabel(/tên đăng nhập|username/i).fill(username);
  await page.getByLabel(/mật khẩu|password/i).fill(password);
  await page.getByRole("button", { name: /đăng nhập/i }).click();
  await page.waitForURL((url) => !url.pathname.startsWith("/login"));
}
