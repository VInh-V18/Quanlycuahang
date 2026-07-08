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
