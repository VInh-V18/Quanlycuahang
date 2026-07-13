import type { Page } from "@playwright/test";

/** Dang nhap qua form that (khong bypass qua localStorage/token) — dam bao luon test dung luong
 * dang nhap that su nguoi dung se di qua (Phase 11 Gate: E2E chinh thuc).
 *
 * <p>Tra ve accessToken (doc thang tu response cua chinh request dang nhap that, khong doan/dua
 * vao Redux) de cac test can goi thang API tao du lieu rieng (vd pos-checkout.spec.ts) co the
 * dinh kem header Authorization — page.request KHONG tu dong mang theo token nay (token nam trong
 * Redux state cua trang, chi refreshToken moi la httpOnly cookie duoc tu dong gui kem). */
export async function login(
  page: Page,
  // Cho phep ghi de qua bien moi truong - mat khau that cua owner01 tren he thong dang chay co
  // the da doi (nguoi dung tu doi qua man hinh Doi mat khau), khi do gia tri hardcode o day sai
  // va CA BO e2e fail o buoc dang nhap (phat hien khi ra soat). Chay bo test tren moi truong nhu
  // vay: E2E_USERNAME=... E2E_PASSWORD=... npx playwright test
  username = process.env.E2E_USERNAME ?? "owner01",
  password = process.env.E2E_PASSWORD ?? "Password@123",
): Promise<string> {
  await page.goto("/login");
  await page.getByLabel(/tên đăng nhập|username/i).fill(username);
  await page.getByLabel(/mật khẩu|password/i).fill(password);
  const [response] = await Promise.all([
    page.waitForResponse(
      (res) => res.url().includes("/api/v1/auth/login") && res.request().method() === "POST",
    ),
    page.getByRole("button", { name: /đăng nhập/i }).click(),
  ]);
  await page.waitForURL((url) => !url.pathname.startsWith("/login"));
  const body = (await response.json()) as { data: { accessToken: string } };
  return body.data.accessToken;
}
