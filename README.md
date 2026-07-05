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

```bash
cp .env.example .env   # rồi điền giá trị thật
docker compose -f docker/docker-compose.yml up -d
```

(Cấu hình Docker Compose sẽ được bổ sung ở Phase 12.)

## Tài liệu

- [`docs/conventions.md`](docs/conventions.md) — quy ước đặt tên, chuẩn API, bảo mật, hiệu năng, toàn vẹn dữ liệu
- [`docs/PROJECT_STATE.md`](docs/PROJECT_STATE.md) — trạng thái dự án theo từng Phase
