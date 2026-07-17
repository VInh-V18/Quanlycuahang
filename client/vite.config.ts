/// <reference types="vitest/config" />
import path from "node:path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    rollupOptions: {
      output: {
        // Khong co manualChunks truoc day (phat hien qua audit production readiness 2026-07-17) -
        // Rollup gop het React/React Router/Redux Toolkit/TanStack Query/Axios/toan bo Radix UI
        // vao 1 chunk entry ~507KB, tai VA parse truoc khi bat ky code theo route nao (da lazy-split
        // dung) bat dau chay. Tach vendor thanh 3 nhom co y nghia — nho hon, VA cache doc lap voi
        // code ung dung (sua 1 trang khong lam nguoi dung tai lai toan bo vendor).
        manualChunks(id) {
          if (!id.includes("node_modules")) return undefined;
          if (id.includes("react-router-dom") || id.match(/node_modules\/react(-dom)?\//)) {
            return "react-vendor";
          }
          if (id.includes("@radix-ui")) {
            return "radix-ui";
          }
          if (
            id.includes("@tanstack/react-query") ||
            id.includes("@reduxjs/toolkit") ||
            id.includes("react-redux") ||
            id.includes("redux-persist") ||
            id.includes("axios")
          ) {
            return "query-redux";
          }
          return undefined;
        },
      },
    },
  },
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    // Mac dinh Vitest gom ca "**/*.spec.ts", trung voi bo Playwright E2E o client/e2e/ (Phase 11)
    // — khien `npm test` bao loi "test() did not expect to be called here" vi Playwright test()
    // bi Vitest import nham. Loai tru rieng thu muc e2e/ (co runner + config rieng, chay qua
    // `npm run test:e2e`).
    exclude: ["e2e/**", "node_modules/**"],
  },
});
