import { defineConfig, devices } from "@playwright/test";

/**
 * E2E chinh thuc (Phase 11 Gate) — chay tren Chromium da cai san trong moi truong
 * (PLAYWRIGHT_BROWSERS_PATH=/opt/pw-browsers), KHONG tai lai browser. Yeu cau backend (:8080) +
 * Postgres/Redis + `npm run dev` (:5173) dang chay truoc khi `npx playwright test`.
 */
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
    launchOptions: {
      executablePath: "/opt/pw-browsers/chromium",
    },
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
