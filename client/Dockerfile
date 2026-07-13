# Multi-stage build (Phase 12) — stage 1 build SPA tinh bang Vite, stage 2 chi la nginx serve
# static + proxy /api sang container server, khong mang theo node_modules/toolchain vao production.

FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# nginx.conf duoc mount qua docker-compose.yml (../docker/nginx.conf) thay vi COPY o day, vi
# COPY khong the doc file ngoai build context (client/) — xem docker/docker-compose.yml.

EXPOSE 80

# 127.0.0.1 (khong phai localhost): wget cua busybox/alpine phan giai localhost ra [::1] (IPv6)
# truoc, trong khi nginx trong container chi nghe IPv4 - healthcheck luon fail "Connection
# refused" va container bao unhealthy du trang van chay binh thuong (phat hien khi ra soat).
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1
