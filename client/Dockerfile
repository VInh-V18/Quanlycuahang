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

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
