# Quản Lý Cửa Hàng — ERP Bán Hàng (KiotViet-inspired)

Web ERP quản lý bán hàng cho cửa hàng bán lẻ vừa và nhỏ tại Việt Nam
(1–5 chi nhánh, 2–20 nhân viên, 500–20.000 SKU, 50–500 đơn/ngày).

## Kiến trúc

- **Backend**: Spring Boot 3.3.x (Java 21) + Spring Data JPA + PostgreSQL 16 + Redis + Flyway
- **Frontend**: React 18 + TypeScript + Vite + Tailwind CSS + shadcn/ui
- **Deploy**: Docker Compose (Nginx + client + server + postgres + redis)

## Cấu trúc thư mục

```
Quanlycuahang/
├── client/     # React SPA (Vite)
├── server/     # Spring Boot (Maven)
├── docker/     # docker-compose, nginx.conf, Dockerfile
├── docs/       # tài liệu dự án, PROJECT_STATE
└── scripts/    # backup, seed, tiện ích
```

Xem quy ước kỹ thuật đầy đủ tại [`docs/conventions.md`](docs/conventions.md)
và trạng thái dự án hiện tại tại [`docs/PROJECT_STATE.md`](docs/PROJECT_STATE.md).

## Yêu cầu môi trường

- JDK 21 (nếu chạy Backend không qua Docker)
- Node.js 20+ (nếu chạy Frontend không qua Docker)
- Docker + Docker Compose (khuyến nghị cho môi trường local đầy đủ)

## Chạy Backend (local, không Docker)

```bash
cd server
cp ../.env.example ../.env   # rồi điền giá trị thật
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Swagger UI: http://localhost:8080/swagger-ui.html
Health check: http://localhost:8080/actuator/health

## Chạy toàn hệ thống bằng Docker Compose

`docker/docker-compose.yml` dựng đủ 4 service: `postgres`, `redis`, `server` (Spring Boot) và
`web` (Nginx phục vụ SPA đã build + reverse proxy `/api` sang `server`). File `.env` cần đặt
ở **thư mục gốc repo** (không phải trong `docker/`), nên khi chạy `docker compose` phải chỉ rõ
`--env-file .env` — nếu không, biến top-level (`${DB_NAME}`, `${WEB_PORT}`, ...) sẽ không được
thay thế vì Compose mặc định chỉ tự tìm `.env` cạnh file compose.

```bash
cp .env.example .env   # rồi điền giá trị thật (JWT_SECRET, mật khẩu DB/Redis, ...)
docker compose --env-file .env -f docker/docker-compose.yml up -d --build
```

Sau khi lên: web (SPA + API proxy) tại `http://localhost:${WEB_PORT:-80}`.

Dừng hệ thống: `docker compose --env-file .env -f docker/docker-compose.yml down`
(thêm `-v` nếu muốn xoá luôn volume dữ liệu Postgres/Redis/uploads — **mất dữ liệu vĩnh viễn**).

### Hai kịch bản triển khai

`docker-compose.yml` dùng chung cho cả 2 kịch bản, chỉ khác giá trị `.env`:

| Biến | VPS (internet-facing, có domain + HTTPS qua Let's Encrypt) | LAN nội bộ (chỉ HTTP, IP tĩnh) |
|---|---|---|
| `AUTH_REFRESH_COOKIE_SECURE` | `true` | `false` (bắt buộc — RFC 6265 chặn Secure cookie qua HTTP) |
| `CORS_ALLOWED_ORIGINS` | `https://<domain-that>` | `http://<ip-lan>` |
| `WEB_PORT` | sau reverse proxy TLS (Nginx/Caddy) | expose trực tiếp (thường `80`) |

Chi tiết kiến trúc từng kịch bản: [`docs/phase1/component-deployment-diagram.md`](docs/phase1/component-deployment-diagram.md).

## Vận hành (Operations)

### Sao lưu / phục hồi database

```bash
./scripts/backup.sh                 # xuất backups/<DB_NAME>-<timestamp>.sql.gz
./scripts/restore.sh backups/quanlycuahang-20260706-120000.sql.gz   # ghi đè database hiện có, có xác nhận
```

Cả 2 script đọc `.env` ở thư mục gốc và thao tác qua `docker compose exec postgres` — cần
container `postgres` đang chạy. Khuyến nghị đặt `backup.sh` chạy định kỳ bằng cron trên máy chủ.

### CI/CD

Pipeline GitHub Actions (`.github/workflows/ci.yml`) chạy trên mọi push/PR, gồm 3 job tuần tự:

1. **backend** — `mvn verify` (unit test Surefire + integration test Testcontainers qua Failsafe
   + kiểm tra format qua Spotless).
2. **frontend** — lint, typecheck, unit test (Vitest), build.
3. **e2e** — dựng Postgres/Redis thật trên runner, build & chạy backend + frontend, chạy bộ
   Playwright E2E chính thức (`client/e2e/`).

## Tài liệu

- [`docs/conventions.md`](docs/conventions.md) — quy ước đặt tên, chuẩn API, bảo mật, hiệu năng, toàn vẹn dữ liệu
- [`docs/PROJECT_STATE.md`](docs/PROJECT_STATE.md) — trạng thái dự án theo từng Phase
- [`docs/phase1/component-deployment-diagram.md`](docs/phase1/component-deployment-diagram.md) — kiến trúc triển khai VPS vs LAN
