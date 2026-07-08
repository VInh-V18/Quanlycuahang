import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Ca & két tiền", () => {
  test("hiển thị form mở ca hoặc thẻ ca đang mở tuỳ trạng thái hiện tại", async ({ page }) => {
    await login(page);
    await page.goto("/shifts");

    await expect(page.getByRole("heading", { name: "Ca & két tiền" })).toBeVisible();

    // Trang co 2 trang thai loai tru nhau tuy da mo ca hay chua — chi can 1 trong 2 hien ra.
    const openShiftForm = page.getByRole("button", { name: "Mở ca" });
    const currentShiftCard = page.getByText("Ca đang mở");
    await expect(openShiftForm.or(currentShiftCard)).toBeVisible();
  });
});
