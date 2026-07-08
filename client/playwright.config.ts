import { existsSync } from "node:fs";
import { defineConfig, devices } from "@playwright/test";

/**
 * E2E chinh thuc (Phase 11/12 Gate). Yeu cau backend (:8080) + Postgres/Redis + `npm run dev`
 * (:5173) dang chay truoc khi `npx playwright test`.
 *
 * executablePath: sandbox phat trien co san Chromium tai /opt/pw-browsers/chromium (khong tai
 * lai duoc vi khong co mang tai trien khai) — CI (GitHub Actions, xem .github/workflows/ci.yml)
 * KHONG co duong dan nay, tu `npx playwright install --with-deps chromium` roi de Playwright tu
 * tim browser no vua tai (mac dinh, khong set executablePath). Chi ep executablePath khi file do
 * ton tai that, tranh crash "executable doesn't exist" tren moi truong khac.
 */
const SANDBOX_CHROMIUM_PATH = "/opt/pw-browsers/chromium";
const launchOptions = existsSync(SANDBOX_CHROMIUM_PATH)
  ? { executablePath: SANDBOX_CHROMIUM_PATH }
  : {};

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    launchOptions,
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
