import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test("thu ngân bán 1 sản phẩm bằng tiền mặt và nhận thông báo thanh toán thành công", async ({
  page,
}) => {
  await login(page);
  await page.goto("/pos");

  await page.getByText("Ca phe hoa tan G7 hop 20 goi").first().click();
  // O "Khach dua" khong co label lien ket (input[type=number] cuoi cung trong khu vuc thanh
  // toan tien mat) — dien du tien de qua duoc validate "Khach dua chua du tien".
  await page.locator("input[type=number]").last().fill("55000");
  await page.getByRole("button", { name: /Thanh toán & In/i }).click();

  // Toast Radix render 2 node cung text (div hien thi + span aria-live cho screen reader).
  await expect(page.getByText(/Đã thanh toán đơn/i).first()).toBeVisible();
});
